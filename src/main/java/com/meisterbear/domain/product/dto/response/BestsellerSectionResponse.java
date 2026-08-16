package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "추천 페이지 베스트셀러 섹션")
public class BestsellerSectionResponse {

    @Schema(description = "섹션 제목", example = "베스트셀러")
    private String title;

    @Schema(description = "베스트셀러 제품 목록")
    private List<ProductSummaryResponse> products;
}
