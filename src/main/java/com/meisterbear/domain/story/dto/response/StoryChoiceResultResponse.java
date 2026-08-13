package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 질문 선택 결과")
public class StoryChoiceResultResponse {

    @Schema(description = "story.id", example = "3")
    private Long storyId;

    @Schema(description = "선택한 choice의 tagName", example = "classic")
    private String tagName;
}
