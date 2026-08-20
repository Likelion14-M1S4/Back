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

    // RestClient는 전체 응답을 다 받아야 해서, 라인 단위 스트리밍엔 원시 HttpClient를 쓴다
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

        // body()를 먼저 열어 try-with-resources에 걸어야 실패 시에도 커넥션이 닫힌다
        try (java.util.stream.Stream<String> lines = response.body()) {
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OpenAI 스트리밍 요청 실패 - status=" + response.statusCode());
            }
            lines.forEach(line -> emitToken(line, onToken));
        }
    }

    // 파싱 실패한 라인은 스트림을 끊지 않고 건너뛴다
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
