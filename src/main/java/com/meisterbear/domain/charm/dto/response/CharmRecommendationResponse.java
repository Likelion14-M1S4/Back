package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참 추천 조회 응답")
public class CharmRecommendationResponse {

    @Schema(description = "화면 상단에 노출할, 사용자가 선택한 참의 상세",
            example = "{\"id\":1,\"name\":\"비세토스 라이언\",\"price\":410000,\"color\":\"꼬냑\","
                    + "\"imgUrl\":\"https://cdn.meisterbear.com/charm/1.png\",\"collectionName\":\"MCM BASIC COLLECTION\","
                    + "\"isPurchasable\":false}")
    private CharmDetailResponse charm;

    @Schema(description = "같은 collection_name(=같은 시즌 참 장식)에 속한, 이 참을 제외한 나머지 참 목록",
            example = "[{\"id\":2,\"name\":\"비세토스 라이언\",\"imgUrl\":\"https://cdn.meisterbear.com/charm/2.png\","
                    + "\"collectionName\":\"MCM BASIC COLLECTION\"}]")
    private List<RecommendedCharmResponse> recommendations;
}
