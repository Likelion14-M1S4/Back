package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 질문")
public class StoryQuestionResponse {

    @Schema(description = "story_question.id", example = "1")
    private Long id;

    @Schema(description = "story_question.question", example = "루나에게 어떤 말을 건넬까요?")
    private String question;

    @Schema(description = "질문에 딸린 선택지 목록")
    private List<StoryChoiceResponse> choices;
}
