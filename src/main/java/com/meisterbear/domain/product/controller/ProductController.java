package com.meisterbear.domain.product.controller;

import com.meisterbear.domain.product.dto.response.ProductDetailResponse;
import com.meisterbear.domain.product.dto.response.RecommendPageResponse;
import com.meisterbear.domain.product.service.ProductService;
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

@Tag(name = "Product", description = "제품 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

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
