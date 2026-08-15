package com.meisterbear.domain.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 POST /oauth/token 응답 중 우리가 쓰는 필드만 매핑 (access_token)
public record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
}
