package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "보유한 참 목록 조회 응답")
public class OwnedCharmListResponse {

    @Schema(description = "컬렉션명 순으로 정렬된 그룹 목록")
    private List<OwnedCharmGroupResponse> collections;

    public static OwnedCharmListResponse empty() {
        return OwnedCharmListResponse.builder()
                .collections(List.of())
                .build();
    }
}
