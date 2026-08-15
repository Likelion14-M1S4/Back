package com.meisterbear.domain.auth.controller;

import com.meisterbear.domain.auth.dto.response.TokenResponse;
import com.meisterbear.domain.auth.exception.AuthErrorCode;
import com.meisterbear.domain.auth.service.DevAuthService;
import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 개발/Swagger 테스트 전용 컨트롤러. dev 프로필에서만 등록되며, 공유 시크릿 헤더로 보호한다.
// (라이브 서버가 dev 프로필로 뜨므로 시크릿이 없으면 이 API는 비활성화된다 - 아무나 토큰을 못 만들게)
@Tag(name = "Auth (dev)", description = "개발 전용 인증 API")
@RestController
@Profile("dev")
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class DevAuthController {

    private static final String SECRET_HEADER = "X-Test-Secret";

    // 미설정(blank)이면 이 엔드포인트를 비활성화한다 - 라이브 오노출 방지
    @Value("${auth.test-token-secret:}")
    private String testTokenSecret;

    private final DevAuthService devAuthService;

    @Operation(
            summary = "[dev] 테스트 토큰 발급",
            description = """
                    카카오 없이 서비스 JWT를 즉시 발급한다. Swagger 우측 상단 Authorize에 넣어 보호된 API를 테스트할 때 쓴다.

                    - `X-Test-Secret` 헤더에 서버에 설정된 공유 시크릿을 넣어야 한다. (미설정 서버에선 비활성화 - 401)
                    - `userId`를 주면 그 유저로, 안 주면 공용 테스트 유저로 토큰을 발급한다. userId를 바꾸면 다른 유저로 로그인한 효과.
                    - dev 프로필에서만 노출된다.""")
    @PostMapping("/test-token")
    public BaseResponse<TokenResponse> issueTestToken(
            @Parameter(description = "발급 대상 userId. 생략 시 공용 테스트 유저") @RequestParam(required = false) Long userId,
            @Parameter(description = "서버 공유 시크릿") @RequestHeader(value = SECRET_HEADER, required = false) String secret) {
        verifySecret(secret);
        TokenResponse response = devAuthService.issueTestToken(userId);
        return BaseResponse.success("테스트 토큰 발급 성공", response);
    }

    // 시크릿이 미설정(blank)이거나 헤더 값이 일치하지 않으면 차단
    private void verifySecret(String provided) {
        if (testTokenSecret == null || testTokenSecret.isBlank() || !testTokenSecret.equals(provided)) {
            throw new CustomException(AuthErrorCode.UNAUTHORIZED);
        }
    }
}
