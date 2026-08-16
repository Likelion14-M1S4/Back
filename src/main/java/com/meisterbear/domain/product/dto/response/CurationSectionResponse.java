package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "추천 페이지 큐레이션 섹션")
public class CurationSectionResponse {

    @Schema(description = "섹션 제목", example = "이달의 큐레이션")
    private String title;

    @Schema(description = "큐레이션 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/recommend/curation.png")
    private String imageUrl;
}
