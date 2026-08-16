package com.meisterbear.domain.product.dto.response;

import com.meisterbear.domain.store.dto.response.StoreHourResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "매장 태그 상세 응답")
public class StoreTagDetailResponse {

    @Schema(description = "매장명", example = "MCM 롯데백화점 본점")
    private String storeName;

    @Schema(description = "주소 (우편번호 포함)", example = "서울 중구 남대문로 81, 롯데백화점 본점 1F 04533")
    private String address;

    @Schema(description = "전화번호", example = "+82-2-772-3198")
    private String phone;

    @Schema(description = "요일별 운영시간")
    private List<StoreHourResponse> hours;

    @Schema(description = "날짜별 태그 제품 그룹 (최근 날짜 순)")
    private List<TaggedGroupResponse> taggedGroups;
}
