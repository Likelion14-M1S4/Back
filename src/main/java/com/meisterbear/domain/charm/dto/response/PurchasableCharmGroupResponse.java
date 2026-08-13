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

    @Schema(description = "이 컬렉션에서 구매 가능한 참 목록")
    private List<PurchasableCharmResponse> charms;
}
