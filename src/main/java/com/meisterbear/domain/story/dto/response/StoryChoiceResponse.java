package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 선택지")
public class StoryChoiceResponse {

    @Schema(description = "story_choice.id", example = "1")
    private Long id;

    @Schema(description = "story_choice.label", example = "반가워!")
    private String label;

    @Schema(description = "story_choice.tag_name", example = "friendly")
    private String tagName;
}
