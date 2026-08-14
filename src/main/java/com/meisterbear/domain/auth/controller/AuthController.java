package com.meisterbear.domain.auth.controller;

import com.meisterbear.domain.auth.dto.request.KakaoLoginRequest;
import com.meisterbear.domain.auth.dto.request.TokenReissueRequest;
import com.meisterbear.domain.auth.dto.response.LoginResponse;
import com.meisterbear.domain.auth.dto.response.TokenResponse;
import com.meisterbear.domain.auth.service.AuthService;
import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 SDK로 발급받은 `kakaoAccessToken`을 받아 로그인/회원가입을 처리하고 서비스 JWT를 발급한다.

                    - 최초 로그인이면 회원을 생성하고 `isNewUser=true`로 응답한다. (닉네임은 카카오 프로필, 이메일은 동의 시에만 저장 - 미동의면 null)
                    - accessToken(60분)/refreshToken(14일) 둘 다 응답 body로 내려주며, 프론트는 localStorage에 저장한다.
                    - 이 API는 인증이 필요 없다(Authorization 헤더 X).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "로그인 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "로그인 성공",
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "isNewUser": true
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 카카오 토큰",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "카카오 토큰 오류", value = """
                                    {
                                      "success": false,
                                      "code": "AUTH401",
                                      "message": "유효하지 않은 카카오 토큰입니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/kakao")
    public BaseResponse<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        LoginResponse response = authService.kakaoLogin(request.getKakaoAccessToken());
        return BaseResponse.success("로그인 성공", response);
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
                    리프레시 토큰으로 새 accessToken/refreshToken을 발급한다(rotate).

                    - refresh 토큰은 DB 저장값과 대조하며, 로그아웃했거나 더 최근 로그인/재발급으로 갱신된 옛 토큰은 거부된다(단일 세션).
                    - 응답으로 **access와 refresh를 둘 다** 새로 내려주므로 프론트는 둘 다 교체 저장한다.
                    - 이 API는 인증이 필요 없다(만료된 access 대신 refresh로 재발급).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "재발급 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "토큰 재발급 성공",
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "리프레시 토큰 오류", value = """
                                    {
                                      "success": false,
                                      "code": "AUTH401",
                                      "message": "유효하지 않은 리프레시 토큰입니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/refresh")
    public BaseResponse<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        TokenResponse response = authService.reissue(request.getRefreshToken());
        return BaseResponse.success("토큰 재발급 성공", response);
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    로그인한 유저의 리프레시 토큰을 무효화한다.

                    - 이후 그 refresh 토큰으로는 재발급이 되지 않는다.
                    - access 토큰은 stateless라 만료(60분)까지 유효하므로, 프론트는 로컬 저장 토큰(access/refresh)을 함께 삭제한다.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "로그아웃 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "로그아웃 성공",
                                      "data": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "인증 필요", value = """
                                    {
                                      "success": false,
                                      "code": "AUTH401",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/logout")
    public BaseResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUser().getId());
        return BaseResponse.success("로그아웃 성공", null);
    }
}
