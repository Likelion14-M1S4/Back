package com.meisterbear.domain.chat.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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

    private final RestClient restClient;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey, @Value("${openai.model}") String model) {
        this.model = model;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
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
}
