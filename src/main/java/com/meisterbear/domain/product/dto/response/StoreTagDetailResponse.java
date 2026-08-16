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

    @Schema(description = "요일별 운영시간",
            example = "[{\"day\":\"월요일\",\"time\":\"10:30 - 20:00\"},{\"day\":\"화요일\",\"time\":\"10:30 - 20:00\"}]")
    private List<StoreHourResponse> hours;

    @Schema(description = "날짜별 태그 제품 그룹 (최근 날짜 순)",
            example = "[{\"date\":\"2026.08.16\",\"products\":[{\"id\":1,\"name\":\"Stark 사이드 스터드 비세토스 백팩\","
                    + "\"imageUrl\":\"https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png\"}]}]")
    private List<TaggedGroupResponse> taggedGroups;
}
