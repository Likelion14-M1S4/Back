package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "컬렉션별 구매 가능 참 그룹")
public class PurchasableCharmGroupResponse {

    @Schema(description = "charm.collection_name", example = "MCM BASIC COLLECTION")
    private String collectionName;

    @Schema(description = "이 컬렉션에서 구매 가능한 참 목록",
            example = "[{\"id\":1,\"name\":\"비세토스 라이언\",\"price\":410000,\"color\":\"꼬냑\","
                    + "\"imgUrl\":\"https://cdn.meisterbear.com/charm/1.png\",\"collectionName\":\"MCM BASIC COLLECTION\"}]")
    private List<PurchasableCharmResponse> charms;
}
