package com.meisterbear.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카카오 로그인 요청")
public class KakaoLoginRequest {

    @NotBlank(message = "kakaoAccessToken은 필수입니다.")
    @Schema(description = "카카오 SDK로 발급받은 액세스 토큰", example = "kakao_access_token_value")
    private String kakaoAccessToken;
}
