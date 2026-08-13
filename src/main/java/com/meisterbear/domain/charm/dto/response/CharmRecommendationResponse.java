package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참 추천 조회 응답")
public class CharmRecommendationResponse {

    @Schema(description = "화면 상단에 노출할, 사용자가 선택한 참의 상세")
    private CharmDetailResponse charm;

    @Schema(description = "같은 collection_name(=같은 시즌 참 장식)에 속한, 이 참을 제외한 나머지 참 목록")
    private List<RecommendedCharmResponse> recommendations;
}
