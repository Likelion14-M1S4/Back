package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "제품 목록 항목")
public class ProductSummaryResponse {

    @Schema(description = "제품 id", example = "1")
    private Long id;

    @Schema(description = "제품명", example = "Stark 사이드 스터드 비세토스 백팩")
    private String name;

    @Schema(description = "가격(원)", example = "1490000")
    private Integer price;

    @Schema(description = "대표 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png")
    private String imageUrl;
}
