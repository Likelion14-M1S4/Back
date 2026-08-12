package com.meisterbear.domain.story.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스토리 선택지 선택 요청")
public class SelectStoryChoiceRequest {

    @NotNull(message = "questionId는 필수입니다.")
    @Schema(description = "story_question.id", example = "1")
    private Long questionId;

    @NotNull(message = "choiceId는 필수입니다.")
    @Schema(description = "story_choice.id", example = "2")
    private Long choiceId;
}
