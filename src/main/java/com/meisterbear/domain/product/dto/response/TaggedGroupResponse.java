package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "날짜별 태그 제품 그룹")
public class TaggedGroupResponse {

    @Schema(description = "태그한 날짜", example = "2026.08.16")
    private String date;

    @Schema(description = "그날 태그한 제품 목록",
            example = "[{\"id\":1,\"name\":\"Stark 사이드 스터드 비세토스 백팩\","
                    + "\"imageUrl\":\"https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png\"}]")
    private List<TaggedProductResponse> products;
}
