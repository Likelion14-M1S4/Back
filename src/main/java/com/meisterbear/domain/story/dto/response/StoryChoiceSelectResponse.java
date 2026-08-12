package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 선택지 저장 응답")
public class StoryChoiceSelectResponse {

    @Schema(description = "user_choice.id", example = "1")
    private Long userChoiceId;

    @Schema(description = "story_choice.tag_name", example = "calm")
    private String tagName;
}
