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
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/season/2026-FALL/hero.png")
    private String heroImageUrl;

    @Schema(description = "시즌 소개 문구", example = "2026 가을, 마이스터베어의 새로운 시즌을 만나보세요.")
    private String description;

    @Schema(description = "시즌 제품 목록")
    private List<ProductSummaryResponse> products;
}
