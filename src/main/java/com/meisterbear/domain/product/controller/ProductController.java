package com.meisterbear.domain.product.controller;

import com.meisterbear.domain.product.dto.response.MyProductDetailResponse;
import com.meisterbear.domain.product.dto.response.MyProductResponse;
import com.meisterbear.domain.product.dto.response.ProductDetailResponse;
import com.meisterbear.domain.product.dto.response.RecommendPageResponse;
import com.meisterbear.domain.product.dto.response.SeasonProductListResponse;
import com.meisterbear.domain.product.dto.response.StoreTagDetailResponse;
import com.meisterbear.domain.product.dto.response.StoreTagHistoryResponse;
import com.meisterbear.domain.product.service.ProductService;
import com.meisterbear.domain.product.service.ProductTagService;
import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "제품 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductTagService productTagService;

    @Operation(
            summary = "추천 페이지 조회",
            description = "추천 탭 화면 전체 구성(히어로 배너·여정·큐레이션·베스트셀러)을 한 번에 반환한다. "
                    + "베스트셀러 products의 id를 제품 상세 조회(GET /api/products/{productId})에 그대로 사용한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 페이지 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "heroImageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/recommend/hero.png",
                                        "heroLinkTo": "/recommend/charms",
                                        "journey": { "title": "마이스터베어와 함께하는 여정", "subtitle": "나만의 참과 캐릭터를 찾아보세요" },
                                        "curation": { "title": "이달의 큐레이션", "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/recommend/curation.png" },
                                        "bestsellers": {
                                          "title": "베스트셀러",
                                          "products": [
                                            { "id": 1, "name": "Stark 사이드 스터드 비세토스 백팩", "price": 1490000, "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png" }
                                          ]
                                        }
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/recommendations")
    public BaseResponse<RecommendPageResponse> getRecommendPage() {
        return BaseResponse.success(productService.getRecommendPage());
    }

    @Operation(
            summary = "시즌 제품 목록 조회",
            description = "특정 시즌의 히어로 배너·소개 문구·제품 목록을 반환한다. 현재 시즌 값: `AW2026` (스토리 도메인의 시즌 코드 규칙 SS/AW{연도}와 동일). "
                    + "존재하지 않는 시즌 값이면 빈 products로 응답한다(에러 아님). "
                    + "각 제품의 상세는 GET /api/products/{productId}로 조회한다(시즌 상세 화면 겸용).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시즌 제품 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "heroImageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/season/AW2026/hero.png",
                                        "description": "2026 가을, 마이스터베어의 새로운 시즌을 만나보세요.",
                                        "products": [
                                          { "id": 301, "name": "비세토스 라이언 참", "price": 410000, "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/301.png" }
                                        ]
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/seasons/{season}")
    public BaseResponse<SeasonProductListResponse> getSeasonProducts(@PathVariable String season) {
        return BaseResponse.success(productService.getSeasonProducts(season));
    }

    @Operation(
            summary = "등록 제품 목록 조회",
            description = "로그인 유저가 구매(등록)한 제품 목록을 최근 구매 순으로 반환한다. "
                    + "각 항목의 id는 구매 기록 id이며, 등록 제품 상세 조회(GET /api/products/my/{orderItemId})에 그대로 사용한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 제품 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": [
                                        {
                                          "id": 1,
                                          "name": "Stark 사이드 스터드 비세토스 백팩",
                                          "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png",
                                          "registeredAt": "2026.08.16"
                                        }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/my")
    public BaseResponse<List<MyProductResponse>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return BaseResponse.success(productService.getMyProducts(userDetails.getUser().getId()));
    }

    @Operation(
            summary = "매장 태그 이력 목록 조회",
            description = "로그인 유저가 태그한 매장 목록을 최근 방문 순으로 반환한다. "
                    + "각 항목의 id(매장 id)를 매장 태그 상세 조회(GET /api/products/tags/{storeId})에 그대로 사용한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장 태그 이력 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": [
                                        { "id": 1, "storeName": "MCM 롯데백화점 본점", "lastVisitedAt": "2026.08.16" }
                                      ]
                                    }
                                    """)))
    })
    @GetMapping("/tags")
    public BaseResponse<List<StoreTagHistoryResponse>> getStoreTagHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return BaseResponse.success(productTagService.getStoreTagHistory(userDetails.getUser().getId()));
    }

    @Operation(
            summary = "매장 태그 상세 조회",
            description = "매장 정보(주소·전화·운영시간)와 그 매장에서 태그한 제품들을 날짜별 그룹으로 반환한다. "
                    + "taggedGroups는 최근 날짜 순이며, 같은 제품이 여러 날짜에 중복 등장할 수 있다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "매장 태그 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "storeName": "MCM 롯데백화점 본점",
                                        "address": "서울 중구 남대문로 81, 롯데백화점 본점 1F 04533",
                                        "phone": "+82-2-772-3198",
                                        "hours": [ { "day": "월요일", "time": "10:30 - 20:00" } ],
                                        "taggedGroups": [
                                          {
                                            "date": "2026.08.16",
                                            "products": [
                                              { "id": 1, "name": "Stark 사이드 스터드 비세토스 백팩", "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png" }
                                            ]
                                          }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "매장 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "매장 없음", value = """
                                    {
                                      "success": false,
                                      "code": "STORE404",
                                      "message": "매장을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/tags/{storeId}")
    public BaseResponse<StoreTagDetailResponse> getStoreTagDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long storeId) {
        StoreTagDetailResponse response =
                productTagService.getStoreTagDetail(userDetails.getUser().getId(), storeId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "등록 제품 상세 조회",
            description = "구매 기록 하나의 상세(색상/사이즈/구매·등록 일시/매장)를 반환한다. "
                    + "{orderItemId}는 등록 제품 목록 응답의 id를 그대로 사용한다. 본인 구매 기록만 조회된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 제품 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "id": 1,
                                        "name": "Stark 사이드 스터드 비세토스 백팩",
                                        "colorLabel": "Soft Pink",
                                        "sizeLabel": "미니",
                                        "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png",
                                        "purchasedAt": "2026.08.16 pm.03:00",
                                        "registeredAt": "2026.08.16 pm.03:30",
                                        "storeName": "MCM 롯데백화점 본점"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "구매 내역 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "구매 내역 없음", value = """
                                    {
                                      "success": false,
                                      "code": "PROD404",
                                      "message": "구매 내역을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/my/{orderItemId}")
    public BaseResponse<MyProductDetailResponse> getMyProductDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderItemId) {
        MyProductDetailResponse response =
                productService.getMyProductDetail(userDetails.getUser().getId(), orderItemId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "제품 상세 조회",
            description = "제품 하나의 상세 정보를 반환한다. 시즌 제품 상세 화면도 이 API를 사용한다.\n\n"
                    + "- colors: 같은 디자인의 색상 옵션들. 각 항목의 id로 이 API를 재호출하면 해당 색상 상세로 전환된다.\n"
                    + "- isLiked/isPurchased: 로그인 유저 기준 찜/구매 여부.\n"
                    + "- requiresStory: 시즌 한정 제품 여부(스토리 완주 후 구매 가능 정책).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "id": 1,
                                        "name": "Stark 사이드 스터드 비세토스 백팩",
                                        "price": 1490000,
                                        "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png",
                                        "isLiked": false,
                                        "colorLabel": "Soft Pink",
                                        "colors": [
                                          { "id": 1, "name": "Soft Pink", "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png" }
                                        ],
                                        "sizes": ["미니", "S"],
                                        "selectedSize": "미니",
                                        "storeCheckLabel": "구매 가능 매장 확인하기",
                                        "storeUrl": "/story/stores",
                                        "detail": {
                                          "headline": "Stark 사이드 스터드 비세토스 백팩",
                                          "description": "비세토스 캔버스에 스터드 디테일을 더한 백팩",
                                          "specs": ["소재: 비세토스 캔버스", "아틀리에: 서울"]
                                        },
                                        "isPurchased": false,
                                        "requiresStory": false
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "제품 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "제품 없음", value = """
                                    {
                                      "success": false,
                                      "code": "PROD404",
                                      "message": "해당 상품을 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/{productId}")
    public BaseResponse<ProductDetailResponse> getProductDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {
        ProductDetailResponse response =
                productService.getProductDetail(userDetails.getUser().getId(), productId);
        return BaseResponse.success(response);
    }
}
