package com.meisterbear.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "JWT 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    // boolean getter(isNewUser)를 Jackson이 "newUser"로 직렬화하는 걸 막고 명세대로 "isNewUser"로 내보낸다
    @JsonProperty("isNewUser")
    @Schema(description = "최초 가입 여부", example = "true")
    private boolean isNewUser;
}
