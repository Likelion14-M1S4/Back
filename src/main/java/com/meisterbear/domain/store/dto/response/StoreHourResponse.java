package com.meisterbear.domain.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "매장 요일별 운영시간")
public class StoreHourResponse {

    @Schema(description = "요일", example = "월요일")
    private String day;

    @Schema(description = "운영시간 범위", example = "10:30 - 20:00")
    private String time;
}
