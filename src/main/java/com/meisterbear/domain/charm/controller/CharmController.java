package com.meisterbear.domain.charm.controller;

import com.meisterbear.domain.charm.dto.response.OwnedCharmListResponse;
import com.meisterbear.domain.charm.service.CharmService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Charm", description = "참 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charms")
public class CharmController {

    // TODO: 로그인(JWT 발급) 구현되면 이 상수와 SecurityConfig의 permitAll("/api/charms/**") 함께 제거
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final CharmService charmService;

    @Operation(
            summary = "보유한 참 목록 조회",
            description = "charm_receipt.status가 COMPLETED(매장에서 실제 수령 완료)인 참만 컬렉션명 기준으로 그룹핑해서 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보유한 참 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "collections": [
                                          {
                                            "collectionName": "MCM BASIC COLLECTION",
                                            "charms": [
                                              {
                                                "id": 1,
                                                "name": "비세토스 라이언",
                                                "imgUrl": "https://cdn.meisterbear.com/charm/1.png",
                                                "collectionName": "MCM BASIC COLLECTION"
                                              }
                                            ]
                                          }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "인증 실패", value = """
                                    {
                                      "success": false,
                                      "code": "G006",
                                      "message": "인증이 필요합니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/owned")
    public BaseResponse<OwnedCharmListResponse> findOwnedCharms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        OwnedCharmListResponse response = charmService.findOwnedCharms(userId);
        return BaseResponse.success(response);
    }
}
