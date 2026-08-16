package com.meisterbear.domain.product.controller;

import com.meisterbear.domain.product.dto.response.RecommendPageResponse;
import com.meisterbear.domain.product.service.ProductService;
import com.meisterbear.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
