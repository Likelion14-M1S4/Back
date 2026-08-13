package com.meisterbear.domain.wishlist.controller;

import com.meisterbear.domain.wishlist.dto.request.ToggleWishlistRequest;
import com.meisterbear.domain.wishlist.dto.response.ToggleWishlistResultResponse;
import com.meisterbear.domain.wishlist.dto.response.WishlistListResponse;
import com.meisterbear.domain.wishlist.service.WishlistService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wishlist", description = "위시리스트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    // TODO: 로그인(JWT 발급) 구현되면 이 상수와 SecurityConfig의 permitAll("/api/wishlist/**") 함께 제거
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final WishlistService wishlistService;

    @Operation(
            summary = "위시리스트 목록 조회",
            description = "로그인한 유저가 찜한 제품·참을 최근 찜한 순으로 반환한다. type으로 PRODUCT/CHARM을 구분하며, "
                    + "targetId는 type에 따라 product.id 또는 charm.id를 가리킨다. "
                    + "찜한 뒤 대상 제품/참이 삭제된 경우 그 항목은 목록에서 조용히 제외된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "위시리스트 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "id": 2,
                                            "type": "PRODUCT",
                                            "targetId": 5,
                                            "name": "Stark 사이드 스터드 비세토스",
                                            "imgUrl": "https://cdn.meisterbear.com/product/5.png",
                                            "price": 1490000
                                          },
                                          {
                                            "id": 1,
                                            "type": "PRODUCT",
                                            "targetId": 3,
                                            "name": "Stark 사이드 스터드 비세토스",
                                            "imgUrl": "https://cdn.meisterbear.com/product/3.png",
                                            "price": 1490000
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
    @GetMapping
    public BaseResponse<WishlistListResponse> findWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        WishlistListResponse response = wishlistService.findWishlist(userId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "위시리스트 찜/해제 토글",
            description = "이미 찜한 상태면 해제(삭제), 아니면 찜(추가)한다. 제품/참 상세의 하트 아이콘과 위시리스트 목록의 X 버튼이 "
                    + "이 API 하나를 공유한다 - 하트를 누르면 isWished=true로 응답이 와서 하트를 채워서 표시하면 되고, "
                    + "위시리스트 목록에서 X를 누르면 isWished=false로 응답이 와서 그 항목을 목록에서 지우면 된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토글 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = {
                                    @ExampleObject(name = "찜 추가됨 (하트 채우기)", value = """
                                            {
                                              "success": true,
                                              "code": 200,
                                              "message": "요청이 성공적으로 처리되었습니다.",
                                              "data": {
                                                "type": "PRODUCT",
                                                "targetId": 5,
                                                "isWished": true
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "찜 해제됨 (목록에서 제거)", value = """
                                            {
                                              "success": true,
                                              "code": 200,
                                              "message": "요청이 성공적으로 처리되었습니다.",
                                              "data": {
                                                "type": "PRODUCT",
                                                "targetId": 5,
                                                "isWished": false
                                              }
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "productId/charmId 둘 다 없거나 둘 다 있음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "잘못된 요청", value = """
                                    {
                                      "success": false,
                                      "code": "WISHLIST400",
                                      "message": "productId와 charmId 중 정확히 하나만 지정해야 합니다.",
                                      "data": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 제품/참",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = {
                                    @ExampleObject(name = "제품 없음", value = """
                                            {
                                              "success": false,
                                              "code": "PROD404",
                                              "message": "해당 상품을 찾을 수 없습니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "참 없음", value = """
                                            {
                                              "success": false,
                                              "code": "CHARM404",
                                              "message": "해당 참을 찾을 수 없습니다.",
                                              "data": null
                                            }
                                            """)
                            }))
    })
    @PostMapping("/toggle")
    public BaseResponse<ToggleWishlistResultResponse> toggleWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ToggleWishlistRequest request) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        ToggleWishlistResultResponse response = wishlistService.toggleWishlist(userId, request);
        return BaseResponse.success(response);
    }
}
