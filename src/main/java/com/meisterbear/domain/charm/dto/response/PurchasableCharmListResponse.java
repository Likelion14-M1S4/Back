package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "구매 가능한 참 목록 조회 응답")
public class PurchasableCharmListResponse {

    @Schema(description = "컬렉션명 순으로 정렬된 그룹 목록. 유저가 현재 시즌 스토리를 전부 완주하지 않았다면 빈 배열")
    private List<PurchasableCharmGroupResponse> collections;

    public static PurchasableCharmListResponse empty() {
        return PurchasableCharmListResponse.builder()
                .collections(List.of())
                .build();
    }
}
