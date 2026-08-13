package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "보유한 참 목록 조회 응답")
public class OwnedCharmListResponse {

    @Schema(description = "컬렉션명 순으로 정렬된 그룹 목록",
            example = "[{\"collectionName\":\"MCM BASIC COLLECTION\",\"charms\":[{\"id\":1,\"name\":\"비세토스 라이언\","
                    + "\"imgUrl\":\"https://cdn.meisterbear.com/charm/1.png\",\"collectionName\":\"MCM BASIC COLLECTION\"}]}]")
    private List<OwnedCharmGroupResponse> collections;

    public static OwnedCharmListResponse empty() {
        return OwnedCharmListResponse.builder()
                .collections(List.of())
                .build();
    }
}
