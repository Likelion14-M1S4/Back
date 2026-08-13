package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 장면")
public class SceneResponse {

    @Schema(description = "장면 순서", example = "1")
    private Integer order;

    @Schema(description = "장면 이미지 URL", example = "https://cdn.meisterbear.com/story/3-1.png")
    private String imgUrl;

    @Schema(description = "장면 본문", example = "스터드 디테일의 실루엣과 혁신적인 가죽 제품은 예술과 기술, 여행이 교차하는 하우스의 정체성을 드러내요.")
    private String content;
}
