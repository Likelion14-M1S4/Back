package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "추천 페이지 조회 응답")
public class RecommendPageResponse {

    @Schema(description = "히어로 배너 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/recommend/hero.png")
    private String heroImageUrl;

    @Schema(description = "히어로 배너 클릭 시 이동할 내부 경로", example = "/recommend/charms")
    private String heroLinkTo;

    @Schema(description = "여정 섹션")
    private JourneySectionResponse journey;

    @Schema(description = "큐레이션 섹션")
    private CurationSectionResponse curation;

    @Schema(description = "베스트셀러 섹션")
    private BestsellerSectionResponse bestsellers;
}
