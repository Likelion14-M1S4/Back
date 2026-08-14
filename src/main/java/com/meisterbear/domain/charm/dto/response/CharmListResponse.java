package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참 목록 조회 응답")
public class CharmListResponse {

    @Schema(description = "전체 참 목록",
            example = "[{\"id\":1,\"name\":\"비세토스 라이언\",\"imgUrl\":\"https://cdn.meisterbear.com/charm/1.png\","
                    + "\"collectionName\":\"MCM BASIC COLLECTION\"}]")
    private List<CharmSummaryResponse> charms;

    public static CharmListResponse empty() {
        return CharmListResponse.builder()
                .charms(List.of())
                .build();
    }
}
