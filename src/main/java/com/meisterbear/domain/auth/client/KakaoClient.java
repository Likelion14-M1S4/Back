package com.meisterbear.domain.auth.client;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class KakaoClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String AUTHORIZE_ENDPOINT = "https://kauth.kakao.com/oauth/authorize";

    private final RestClient apiClient;   // kapi.kakao.com
    private final RestClient authClient;  // kauth.kakao.com
    private final String clientId;
    private final String clientSecret;
    private final String defaultRedirectUri;

    public KakaoClient(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret}") String clientSecret,
            @Value("${kakao.redirect-uri}") String defaultRedirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.defaultRedirectUri = defaultRedirectUri;
        this.apiClient = buildClient("https://kapi.kakao.com");
        this.authClient = buildClient("https://kauth.kakao.com");
        if (clientId == null || clientId.isBlank()) {
            log.warn("[KakaoClient] KAKAO_CLIENT_ID 미설정 - 카카오 로그인 전체가 동작하지 않습니다");
        }
        if (defaultRedirectUri == null || defaultRedirectUri.isBlank()) {
            log.warn("[KakaoClient] KAKAO_REDIRECT_URI 미설정 - 콜백 방식 로그인(/api/auth/kakao/authorize)이 동작하지 않습니다");
        }
    }

    private static RestClient buildClient(String baseUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    // state에 복귀할 프론트 origin을 싣는다 (redirect_uri엔 임의 파라미터를 못 붙임)
    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_ENDPOINT)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", defaultRedirectUri)
                .queryParam("state", state)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    // 예외를 AuthErrorCode로 변환하는 건 호출부(AuthService) 책임
    public KakaoTokenResponse getToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", resolveRedirectUri(redirectUri));
        form.add("code", code);
        // client_secret 활성화 시 필수 (누락하면 KOE010)
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        return authClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);
    }

    public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
        return apiClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }

    private String resolveRedirectUri(String redirectUri) {
        return (redirectUri == null || redirectUri.isBlank()) ? defaultRedirectUri : redirectUri;
    }
}
