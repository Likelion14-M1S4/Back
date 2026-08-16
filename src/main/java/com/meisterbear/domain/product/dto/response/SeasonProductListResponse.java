package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "시즌 제품 목록 조회 응답")
public class SeasonProductListResponse {

    @Schema(description = "시즌 히어로 배너 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/season/AW2026/hero.png")
    private String heroImageUrl;

    @Schema(description = "시즌 소개 문구", example = "마이스터베어의 새로운 시즌을 만나보세요.")
    private String description;

    @Schema(description = "시즌 제품 목록",
            example = "[{\"id\":301,\"name\":\"비세토스 라이언 참\",\"price\":410000,"
                    + "\"imageUrl\":\"https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/301.png\"}]")
    private List<ProductSummaryResponse> products;
}
