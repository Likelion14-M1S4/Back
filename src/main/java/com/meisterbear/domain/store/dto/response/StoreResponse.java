package com.meisterbear.domain.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "구매 가능 매장 응답")
public class StoreResponse {

    @Schema(description = "매장 id", example = "1")
    private Long id;

    @Schema(description = "매장명", example = "MCM 롯데백화점 본점")
    private String name;

    @Schema(description = "주소", example = "서울 중구 남대문로 81, 롯데백화점 본점 1F")
    private String address;

    @Schema(description = "우편번호", example = "04533")
    private String postalCode;

    @Schema(description = "전화번호", example = "+82-2-772-3198")
    private String phone;

    @Schema(description = "요일별 운영시간 (7요일)")
    private List<StoreHourResponse> hours;
}
