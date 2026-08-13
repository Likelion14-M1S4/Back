package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "질문 선택지")
public class ChoiceResponse {

    @Schema(description = "선택지 id", example = "2")
    private Long id;

    @Schema(description = "선택지 문구", example = "이번 시즌 컬렉션의 주목해야할 부분은?")
    private String label;
}
