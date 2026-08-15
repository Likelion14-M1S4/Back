package com.meisterbear.domain.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 GET /v2/user/me 응답 중 우리가 쓰는 필드만 매핑 (id, 이메일, 닉네임)
public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

    public record KakaoAccount(String email, Profile profile) {

        public record Profile(String nickname) {
        }
    }
}
