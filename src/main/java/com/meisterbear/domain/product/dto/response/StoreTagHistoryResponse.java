package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "매장 태그 이력 목록 항목")
public class StoreTagHistoryResponse {

    @Schema(description = "매장 id (태그 상세 조회에 이 값을 사용)", example = "1")
    private Long id;

    @Schema(description = "매장명", example = "MCM 롯데백화점 본점")
    private String storeName;

    @Schema(description = "마지막 방문(태그)일", example = "2026.08.16")
    private String lastVisitedAt;
}
