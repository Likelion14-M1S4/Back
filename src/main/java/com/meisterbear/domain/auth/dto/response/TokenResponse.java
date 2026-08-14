package com.meisterbear.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "토큰 재발급 응답")
public class TokenResponse {

    @Schema(description = "새로 발급된 JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "새로 발급된 JWT 리프레시 토큰 (rotate)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
