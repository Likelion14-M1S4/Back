package com.meisterbear.domain.auth.client;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoClient {

    // 카카오 서버가 느리거나 응답이 안 오는 상황에서 요청 스레드가 무한정 잡혀있지 않도록 명시적 타임아웃을 둔다
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    public KakaoClient() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .requestFactory(requestFactory)
                .build();
    }

    // 카카오 액세스 토큰으로 사용자 정보를 조회한다. 토큰이 유효하지 않으면 4xx, 카카오 장애면 5xx 예외가 그대로 올라간다
    // - 예외를 AuthErrorCode로 변환하는 건 호출부(AuthService)의 책임 (OpenAiClient와 동일한 정책)
    public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
        return restClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }
}
