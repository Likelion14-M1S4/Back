package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "제품 색상 옵션 (같은 디자인의 색상 형제 제품)")
public class ProductColorResponse {

    @Schema(description = "해당 색상 제품의 id (클릭 시 이 id로 상세 재조회)", example = "2")
    private Long id;

    @Schema(description = "색상명", example = "Soft Pink")
    private String name;

    @Schema(description = "해당 색상 제품 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/2.png")
    private String imageUrl;
}
