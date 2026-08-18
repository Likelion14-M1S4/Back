package com.meisterbear.domain.auth.controller;

import com.meisterbear.domain.auth.client.KakaoClient;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final KakaoClient kakaoClient;

    // 로그인 완료 후 복귀를 허용할 프론트 origin 목록 (콤마 구분 프로퍼티 → List 자동 변환)
    @Value("${kakao.front-redirect-uris}")
    private List<String> frontRedirectUris;

    // 허용 목록에 있는 origin만 통과, 그 외(null·미등록)는 첫 항목으로 대체 - state 오픈 리다이렉트 방지
    private String resolveFrontOrigin(String origin) {
        if (origin != null && frontRedirectUris.contains(origin)) {
            return origin;
        }
        return frontRedirectUris.get(0);
    }

    @Operation(
            summary = "카카오 인가 요청 리다이렉트",
            description = """
                    브라우저를 카카오 인가 페이지로 302 리다이렉트한다. (브라우저 내비게이션 전용 - Swagger에서 실행 불가)

                    - 프론트는 로그인 버튼에서 이 URL로 이동만 하면 된다: `/api/auth/kakao/authorize?redirect={프론트 origin}`
                    - `redirect`는 허용 목록 검증 후 state로 카카오에 전달되어, 로그인 완료 시 복귀 주소로 쓰인다.
                    - 허용 목록에 없거나 생략하면 기본 주소(목록 첫 항목)로 복귀한다.
                    - 이 API는 인증이 필요 없다(Authorization 헤더 X).""")
    @GetMapping("/kakao/authorize")
    public void kakaoAuthorize(@RequestParam(required = false) String redirect,
                               HttpServletResponse response) throws IOException {
        String frontOrigin = resolveFrontOrigin(redirect);
        response.sendRedirect(kakaoClient.buildAuthorizeUrl(frontOrigin));
    }

    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 인가 코드(`code`)를 받아 로그인/회원가입을 처리하고 서비스 JWT를 발급한다. (토큰 교환은 백엔드가 수행)

                    - 프론트: `Kakao.Auth.authorize({redirectUri})` → 콜백으로 받은 `?code=` 값을 그대로 전달한다. 브라우저에서 토큰 교환 금지.
                    - `redirectUri`는 authorize에 사용한 값과 동일해야 한다(카카오 앱에 등록 필수). 생략 시 서버 설정값 사용.
                    - 인가 코드는 1회용·10분 만료라 재사용 시 401(AUTH401)이 난다.
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
            @ApiResponse(responseCode = "401", description = "유효하지 않은 카카오 인가 코드",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "카카오 인가 코드 오류", value = """
                                    {
                                      "success": false,
                                      "code": "AUTH401",
                                      "message": "유효하지 않은 카카오 인가 코드입니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/kakao")
    public BaseResponse<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        LoginResponse response = authService.kakaoLogin(request.getCode(), request.getRedirectUri());
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

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    로그인한 유저를 하드 삭제한다.

                    - 유저 소유 데이터(컬렉션·스토리 진행·태그·위시리스트·주문)를 삭제한다.
                    - 참 수령 기록(charm_receipt)은 매장측 기록이라 계정 연결만 해제(user_id=null)하고 보존한다.
                    - 복구 불가.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "탈퇴 완료", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "탈퇴 완료",
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
    @DeleteMapping("/withdraw")
    public BaseResponse<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.withdraw(userDetails.getUser().getId());
        return BaseResponse.success("탈퇴 완료", null);
    }
}
