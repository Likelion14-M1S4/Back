package com.meisterbear.domain.charm.controller;

import com.meisterbear.domain.charm.dto.response.CharmDetailResponse;
import com.meisterbear.domain.charm.dto.response.CharmListResponse;
import com.meisterbear.domain.charm.dto.response.CharmRecommendationResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmListResponse;
import com.meisterbear.domain.charm.dto.response.PurchasableCharmListResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Charm", description = "참 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charms")
public class CharmController {

    private final CharmService charmService;

    @Operation(
            summary = "참 목록 조회",
            description = "이 유저가 uid로 태그해서 수집(collect)한 캐릭터의 참만 id 오름차순으로 카드 형태 목록으로 반환한다. "
                    + "캐릭터=참 1:1이므로, 수집한 캐릭터가 없으면 빈 목록을 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "charms": [
                                          {
                                            "id": 1,
                                            "characterId": 1,
                                            "name": "비세토스 라이언",
                                            "imgUrl": "https://cdn.meisterbear.com/charm/1.png",
                                            "collectionName": "MCM BASIC COLLECTION"
                                          },
                                          {
                                            "id": 2,
                                            "characterId": 3,
                                            "name": "루카 라이언",
                                            "imgUrl": "https://cdn.meisterbear.com/charm/2.png",
                                            "collectionName": "MCM BASIC COLLECTION"
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
    public BaseResponse<CharmListResponse> findAllCharms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        CharmListResponse response = charmService.findAllCharms(userId);
        return BaseResponse.success(response);
    }

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
                                                "characterId": 1,
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
        Long userId = userDetails.getUser().getId();
        OwnedCharmListResponse response = charmService.findOwnedCharms(userId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "구매(수령) 가능한 참 목록 조회",
            description = "시즌 한정 참(isSeasonLimited=true) 중, 참마다 연결된 캐릭터×시즌 스토리(charm.character_id, charm.season)를 "
                    + "이 유저가 전부 완주한 참만 반환한다. 일반 참은 이 목록 대상이 아니다. "
                    + "이미 매장에서 수령 완료(COMPLETED)한 참은 목록에서 제외되며, 나머지는 컬렉션명 기준으로 그룹핑해서 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구매 가능한 참 목록 조회 성공",
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
                                                "characterId": 1,
                                                "name": "비세토스 라이언",
                                                "price": 410000,
                                                "color": "꼬냑",
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
    @GetMapping("/purchasable")
    public BaseResponse<PurchasableCharmListResponse> findPurchasableCharms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        PurchasableCharmListResponse response = charmService.findPurchasableCharms(userId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "참 상세 조회",
            description = "참 하나의 상세 정보를 조회한다. 응답의 isSeasonLimited 값으로 프론트에서 화면을 분기한다.\n\n"
                    + "- isSeasonLimited가 true면 시즌 한정 참으로, character는 null이고 isPurchasable 값으로 구매 버튼 상태가 정해진다. "
                    + "isPurchasable은 이 참에 연결된 캐릭터×시즌 스토리(charm.character_id, charm.season)를 이 유저가 전부 완주했는지로 판단하며, "
                    + "false면 '스토리 진행 후 구매 가능' 버튼, true면 '구매 가능' 버튼이 뜬다.\n"
                    + "- isSeasonLimited가 false면 일반 참으로, isPurchasable은 null이고 character 정보로 '캐릭터와 대화하기' 버튼과 "
                    + "캐릭터 소개(personality)가 뜬다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = {
                                    @ExampleObject(name = "조회 성공 - 시즌 한정 참", value = """
                                            {
                                              "success": true,
                                              "code": 200,
                                              "message": "요청이 성공적으로 처리되었습니다.",
                                              "data": {
                                                "id": 1,
                                                "characterId": 1,
                                                "name": "MCM 비세토스 라이언 참",
                                                "price": 410000,
                                                "color": "꼬냑",
                                                "imgUrl": "https://cdn.meisterbear.com/charm/1.png",
                                                "description": "MCM의 상징성을 담아낸 라이언 참은 시그니처 비세토스 패턴과 정교한 가죽 디테일을 조화롭게 담아낸 아이코닉 액세서리입니다.",
                                                "collectionName": "MCM BASIC COLLECTION",
                                                "isSeasonLimited": true,
                                                "isPurchasable": false,
                                                "character": null
                                              }
                                            }
                                            """),
                                    @ExampleObject(name = "조회 성공 - 일반 참", value = """
                                            {
                                              "success": true,
                                              "code": 200,
                                              "message": "요청이 성공적으로 처리되었습니다.",
                                              "data": {
                                                "id": 2,
                                                "characterId": 3,
                                                "name": "비세토스 라이언",
                                                "price": null,
                                                "color": null,
                                                "imgUrl": "https://cdn.meisterbear.com/charm/2.png",
                                                "description": null,
                                                "collectionName": "MCM BASIC COLLECTION",
                                                "isSeasonLimited": false,
                                                "isPurchasable": null,
                                                "character": {
                                                  "id": 3,
                                                  "name": "비세토스 라이언",
                                                  "personality": "항상 침착하고 여유로운 태도를 유지하며, 화려하게 자신을 드러내기보다 자연스럽게 존재감을 보여줍니다.",
                                                  "intro": "독일 뮌헨의 정신을 이어받은 MCM의 상징적인 라이언 캐릭터입니다."
                                                }
                                              }
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 참",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "참 없음", value = """
                                    {
                                      "success": false,
                                      "code": "CHARM404",
                                      "message": "해당 참을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/{charmId}")
    public BaseResponse<CharmDetailResponse> findCharmDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long charmId) {
        Long userId = userDetails.getUser().getId();
        CharmDetailResponse response = charmService.findCharmDetail(userId, charmId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "참 추천 조회",
            description = "상단에는 선택한 참의 상세(charm)를, 하단에는 같은 collection_name×season(=같은 시즌 참 장식)에 속한 "
                    + "나머지 참 목록(recommendations)을 함께 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참 추천 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "charm": {
                                          "id": 1,
                                          "characterId": 1,
                                          "name": "비세토스 라이언",
                                          "price": 410000,
                                          "color": "꼬냑",
                                          "imgUrl": "https://cdn.meisterbear.com/charm/1.png",
                                          "description": "MCM의 상징성을 담아낸 라이언 참은 시그니처 비세토스 패턴과 정교한 가죽 디테일을 조화롭게 담아낸 아이코닉 액세서리입니다.",
                                          "collectionName": "MCM BASIC COLLECTION",
                                          "isPurchasable": false
                                        },
                                        "recommendations": [
                                          {
                                            "id": 2,
                                            "characterId": 3,
                                            "name": "비세토스 라이언",
                                            "imgUrl": "https://cdn.meisterbear.com/charm/2.png",
                                            "collectionName": "MCM BASIC COLLECTION"
                                          }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 참",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "참 없음", value = """
                                    {
                                      "success": false,
                                      "code": "CHARM404",
                                      "message": "해당 참을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/{charmId}/recommendations")
    public BaseResponse<CharmRecommendationResponse> findCharmRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long charmId) {
        Long userId = userDetails.getUser().getId();
        CharmRecommendationResponse response = charmService.findCharmRecommendations(userId, charmId);
        return BaseResponse.success(response);
    }
}
