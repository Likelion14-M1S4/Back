package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "제품 상세 설명 블록")
public class ProductDetailSectionResponse {

    @Schema(description = "상세 헤드라인", example = "Stark 사이드 스터드 비세토스 백팩")
    private String headline;

    @Schema(description = "상세 설명", example = "비세토스 캔버스에 스터드 디테일을 더한 백팩")
    private String description;

    @Schema(description = "스펙 목록", example = "[\"소재: 비세토스 캔버스\", \"아틀리에: 서울\"]")
    private List<String> specs;
}
