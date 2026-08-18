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

    // 카카오 서버가 느리거나 응답이 안 오는 상황에서 요청 스레드가 무한정 잡혀있지 않도록 명시적 타임아웃을 둔다
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String AUTHORIZE_ENDPOINT = "https://kauth.kakao.com/oauth/authorize";

    private final RestClient apiClient;   // kapi.kakao.com - 사용자 정보 조회
    private final RestClient authClient;  // kauth.kakao.com - 인가 코드 → 토큰 교환
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
        // 콜백 플로우는 이 값이 필수지만, redirectUri를 직접 넘기는 POST 플로우는 없이도 동작하므로
        // 기동은 막지 않고 경고만 남긴다 (미설정 시 인가 단계에서 KOE 에러로 나타나는 원인 추적용)
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

    // 카카오 인가 페이지 URL을 조립한다. state에는 로그인 완료 후 복귀할 프론트 origin을 싣는다
    // - redirect_uri에는 임의 쿼리 파라미터를 붙일 수 없다는 카카오 정책 때문에 복귀 주소는 state로 전달한다
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

    // 인가 코드를 카카오 액세스 토큰으로 교환한다. 토큰 교환은 서버에서만 가능하며 client_id는 REST API 키를 쓴다.
    // 코드가 무효/만료/redirect_uri 불일치면 4xx, 카카오 장애면 5xx 예외가 그대로 올라간다
    // - 예외를 AuthErrorCode로 변환하는 건 호출부(AuthService)의 책임 (getUserInfo와 동일한 정책)
    public KakaoTokenResponse getToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", resolveRedirectUri(redirectUri));
        form.add("code", code);
        // 콘솔 [플랫폼 키 > REST API 키 > 클라이언트 시크릿]이 활성화(ON)면 필수 - 누락 시 KOE010(Bad client credentials)
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

    // 카카오 액세스 토큰으로 사용자 정보를 조회한다. 토큰이 유효하지 않으면 4xx, 카카오 장애면 5xx 예외가 그대로 올라간다
    // - 예외를 AuthErrorCode로 변환하는 건 호출부(AuthService)의 책임 (OpenAiClient와 동일한 정책)
    public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
        return apiClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }

    // 프론트가 authorize에 쓴 redirectUri를 우선 사용하고(로컬/배포 도메인이 달라질 수 있음), 없으면 서버 설정값으로 대체.
    // 등록되지 않은 값은 카카오가 교환 단계에서 거부하므로 요청값을 받아도 안전하다
    private String resolveRedirectUri(String redirectUri) {
        return (redirectUri == null || redirectUri.isBlank()) ? defaultRedirectUri : redirectUri;
    }
}
