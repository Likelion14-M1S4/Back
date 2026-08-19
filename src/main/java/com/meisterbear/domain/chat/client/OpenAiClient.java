package com.meisterbear.domain.chat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class OpenAiClient {

    // 커넥션이 안 맺어지거나 응답이 안 오는 상황에서 요청 스레드가 무한정 잡혀있지 않도록 명시적 타임아웃을 둔다
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://api.openai.com/v1/chat/completions");

    private final RestClient restClient;
    private final HttpClient streamingHttpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey, @Value("${openai.model}") String model,
                         ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        HttpClient jdkHttpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.streamingHttpClient = jdkHttpClient;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    // 실패하면 예외를 그대로 던진다 - 캐릭터 톤 대체 문구로 바꾸는 건 호출부(ChatService)의 책임
    public String chat(String systemPrompt, List<ChatTurn> history, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        OpenAiChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(Map.of("model", model, "messages", messages))
                .retrieve()
                .body(OpenAiChatResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 choices가 없습니다.");
        }
        return response.choices().get(0).message().content();
    }

    // 이미지를 base64 data URL로 인코딩해서 마지막 유저 턴에 실어 보낸다 (비전 입력). 텍스트는 안 받고 사진만 분석한다
    public String chatWithImage(String systemPrompt, List<ChatTurn> history, byte[] imageBytes, String contentType) {
        String dataUrl = "data:" + (contentType != null ? contentType : "image/jpeg") + ";base64,"
                + Base64.getEncoder().encodeToString(imageBytes);

        List<Map<String, Object>> imageContent = List.of(
                Map.of("type", "text", "text", "(사진을 보냈어요)"),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
        );

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", imageContent));

        OpenAiChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(Map.of("model", model, "messages", messages))
                .retrieve()
                .body(OpenAiChatResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 choices가 없습니다.");
        }
        return response.choices().get(0).message().content();
    }

    // 스트리밍 응답 - OpenAI가 토큰을 만드는 대로 SSE 라인으로 흘려주는 걸 그대로 받아서 onToken에 넘겨준다.
    // RestClient는 전체 응답을 다 받은 뒤에야 body()가 반환되므로 못 쓰고, 라인 단위로 바로바로 처리할 수 있는
    // 원시 HttpClient(BodyHandlers.ofLines())를 직접 쓴다. 실패하면 예외를 그대로 던진다(대체 문구 처리는 호출부 책임).
    public void chatStream(String systemPrompt, List<ChatTurn> history, String userMessage, Consumer<String> onToken)
            throws IOException, InterruptedException {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        sendStreamingRequest(messages, onToken);
    }

    // chatWithImage의 스트리밍 버전 - 사진 분석(케어 진단)도 토큰 단위로 흘려보낸다
    public void chatWithImageStream(String systemPrompt, List<ChatTurn> history, byte[] imageBytes,
                                     String contentType, Consumer<String> onToken)
            throws IOException, InterruptedException {
        String dataUrl = "data:" + (contentType != null ? contentType : "image/jpeg") + ";base64,"
                + Base64.getEncoder().encodeToString(imageBytes);
        List<Map<String, Object>> imageContent = List.of(
                Map.of("type", "text", "text", "(사진을 보냈어요)"),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
        );

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", imageContent));
        sendStreamingRequest(messages, onToken);
    }

    private void sendStreamingRequest(List<Map<String, Object>> messages, Consumer<String> onToken)
            throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("model", model, "messages", messages, "stream", true));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(CHAT_COMPLETIONS_URI)
                .timeout(READ_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<java.util.stream.Stream<String>> response =
                streamingHttpClient.send(request, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("OpenAI 스트리밍 요청 실패 - status=" + response.statusCode());
        }

        try (java.util.stream.Stream<String> lines = response.body()) {
            lines.forEach(line -> emitToken(line, onToken));
        }
    }

    // SSE 한 줄("data: {...}" 또는 "data: [DONE]")을 파싱해서 delta.content가 있으면 콜백으로 넘긴다.
    // 파싱 실패한 개별 라인은 전체 스트림을 끊지 않고 건너뛴다(다음 토큰은 정상 처리되도록).
    private void emitToken(String line, Consumer<String> onToken) {
        if (line.isBlank() || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring(5).trim();
        if (data.equals("[DONE]")) {
            return;
        }
        try {
            JsonNode delta = objectMapper.readTree(data).path("choices").path(0).path("delta").path("content");
            if (delta.isTextual() && !delta.asText().isEmpty()) {
                onToken.accept(delta.asText());
            }
        } catch (Exception e) {
            log.warn("[OpenAiClient] 스트리밍 청크 파싱 실패 - line={}, error={}", line, e.getMessage());
        }
    }
}
