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

    @Schema(description = "여정 섹션",
            example = "{\"title\":\"마이스터베어와 함께하는 여정\",\"subtitle\":\"나만의 참과 캐릭터를 찾아보세요\"}")
    private JourneySectionResponse journey;

    @Schema(description = "큐레이션 섹션",
            example = "{\"title\":\"이달의 큐레이션\",\"imageUrl\":\"https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/recommend/curation.png\"}")
    private CurationSectionResponse curation;

    @Schema(description = "베스트셀러 섹션",
            example = "{\"title\":\"베스트셀러\",\"products\":[{\"id\":1,\"name\":\"Stark 사이드 스터드 비세토스 백팩\",\"price\":1490000,"
                    + "\"imageUrl\":\"https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png\"}]}")
    private BestsellerSectionResponse bestsellers;
}
