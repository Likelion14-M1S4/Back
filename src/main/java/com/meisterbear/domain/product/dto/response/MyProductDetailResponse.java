package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "등록(구매) 제품 상세 응답")
public class MyProductDetailResponse {

    @Schema(description = "구매 기록 id", example = "1")
    private Long id;

    @Schema(description = "제품명", example = "Stark 사이드 스터드 비세토스 백팩")
    private String name;

    @Schema(description = "색상", example = "Soft Pink")
    private String colorLabel;

    @Schema(description = "사이즈", example = "미니")
    private String sizeLabel;

    @Schema(description = "대표 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png")
    private String imageUrl;

    @Schema(description = "구매일시 (화면 표기 포맷)", example = "2026.08.16 pm.03:00")
    private String purchasedAt;

    @Schema(description = "등록일시 (수령 시점, 화면 표기 포맷)", example = "2026.08.16 pm.03:00")
    private String registeredAt;

    @Schema(description = "구매 매장명", example = "MCM 롯데백화점 본점")
    private String storeName;
}
