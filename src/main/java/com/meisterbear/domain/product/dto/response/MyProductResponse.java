package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "등록(구매) 제품 목록 항목")
public class MyProductResponse {

    @Schema(description = "구매 기록 id (상세 조회에 이 값을 사용)", example = "1")
    private Long id;

    @Schema(description = "제품명", example = "Stark 사이드 스터드 비세토스 백팩")
    private String name;

    @Schema(description = "대표 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png")
    private String imageUrl;

    @Schema(description = "등록일 (화면 표기 포맷)", example = "2026.08.16")
    private String registeredAt;
}
