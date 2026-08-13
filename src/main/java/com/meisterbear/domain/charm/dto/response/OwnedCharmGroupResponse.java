package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "컬렉션별 보유 참 그룹")
public class OwnedCharmGroupResponse {

    @Schema(description = "charm.collection_name", example = "MCM BASIC COLLECTION")
    private String collectionName;

    @Schema(description = "이 컬렉션에서 보유한 참 목록",
            example = "[{\"id\":1,\"name\":\"비세토스 라이언\",\"imgUrl\":\"https://cdn.meisterbear.com/charm/1.png\","
                    + "\"collectionName\":\"MCM BASIC COLLECTION\"}]")
    private List<OwnedCharmResponse> charms;
}
