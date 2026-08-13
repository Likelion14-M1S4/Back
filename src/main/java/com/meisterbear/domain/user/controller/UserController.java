package com.meisterbear.domain.user.controller;

import com.meisterbear.domain.user.dto.request.UpdateUserRequest;
import com.meisterbear.domain.user.dto.response.UserResponse;
import com.meisterbear.domain.user.service.UserService;
import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "유저 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    // TODO: 로그인(JWT 발급) 구현되면 이 상수와 SecurityConfig의 permitAll("/api/users/**") 함께 제거
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final UserService userService;

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 유저 본인의 이메일/닉네임/전화번호/생년월일을 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "email": "seeunbag33@skuniv.ac.kr",
                                        "nickname": "박세은",
                                        "phone": "010 9973 7761",
                                        "birthDate": "2000-00-00"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "유저 없음", value = """
                                    {
                                      "success": false,
                                      "code": "USER404",
                                      "message": "해당 유저를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/me")
    public BaseResponse<UserResponse> findMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        UserResponse response = userService.findMe(userId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "내 정보 수정",
            description = """
                    로그인한 유저 본인의 닉네임/전화번호/생년월일을 수정한다. 이메일은 카카오 연동 값이라 여기서 수정하지 않는다.

                    **phone (전화번호)**
                    - 값을 보낸 경우, 서버가 숫자가 아닌 문자를 전부 제거한 뒤 자릿수만 검사한다. 즉 `"010-9973-7761"`, `"010 9973 7761"`, `"01099737761"` 전부 유효한 입력이고 동일하게 처리된다 - 프론트에서 하이픈/공백을 미리 맞춰서 보낼 필요가 없다.
                    - 숫자로 뽑았을 때 10자리 또는 11자리가 아니면 400(USER400)이 난다.
                    - 저장/응답은 항상 서버가 10자리면 3-3-4, 11자리면 3-4-4로 공백을 넣어 통일한 형식으로 나간다 (예: `"010 9973 7761"`). 화면에 다른 구분자로 보여줘야 한다면 그건 응답을 받은 뒤 프론트에서 처리할 부분이다.

                    **birthDate (생년월일)**
                    - `YYYY-MM-DD` 형식(4자리-2자리-2자리, 구분자는 하이픈 고정)으로만 보내야 한다. 다른 구분자(`.`, `/`, 공백 등)나 자릿수가 다르면 400(USER400)이 난다.
                    - 카카오 로그인에서 생일 정보에 동의하지 않으면 연도만 알고 월/일을 모를 수 있다 - 이 경우 월/일 자리에 `00`을 넣어 보내면 된다(예: `"2000-00-00"`). 실제 캘린더상 유효한 날짜인지(2월 30일 등)는 검사하지 않는다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "수정 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "email": "seeunbag33@skuniv.ac.kr",
                                        "nickname": "박세은",
                                        "phone": "010 9973 7761",
                                        "birthDate": "2000-00-00"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "전화번호/생년월일 형식 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = {
                                    @ExampleObject(name = "전화번호 형식 오류", value = """
                                            {
                                              "success": false,
                                              "code": "USER400",
                                              "message": "전화번호는 숫자 10~11자리여야 합니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "생년월일 형식 오류", value = """
                                            {
                                              "success": false,
                                              "code": "USER400",
                                              "message": "생년월일 형식이 올바르지 않습니다. (예: 2000-00-00)",
                                              "data": null
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "유저 없음", value = """
                                    {
                                      "success": false,
                                      "code": "USER404",
                                      "message": "해당 유저를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PatchMapping("/me")
    public BaseResponse<UserResponse> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateUserRequest request) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        UserResponse response = userService.updateMe(userId, request);
        return BaseResponse.success(response);
    }
}
