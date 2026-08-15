package com.meisterbear.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카카오 로그인 요청 (인가 코드 방식)")
public class KakaoLoginRequest {

    @NotBlank(message = "code는 필수입니다.")
    @Schema(description = "카카오 인가 코드 - Kakao.Auth.authorize 후 redirectUri로 전달된 ?code= 값",
            example = "q1w2E3r4T5y6...")
    private String code;

    @Schema(description = "authorize 호출에 사용한 redirectUri (카카오 앱에 등록된 값과 동일해야 함. 생략 시 서버 설정값 사용)",
            example = "http://localhost:5173/oauth/kakao")
    private String redirectUri;
}
