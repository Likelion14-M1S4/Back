package com.meisterbear.domain.story.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "챕터 질문 선택지 응답 요청")
public class SelectStoryChoiceRequest {

    @Schema(description = "선택한 choice.id", example = "2")
    private Long choiceId;
}
