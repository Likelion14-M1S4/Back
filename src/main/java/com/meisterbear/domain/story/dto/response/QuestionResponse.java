package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 마지막 질문")
public class QuestionResponse {

    @Schema(description = "질문 문구", example = "무엇이 궁금하신가요?")
    private String question;

    @Schema(description = "선택지 목록")
    private List<ChoiceResponse> choices;
}
