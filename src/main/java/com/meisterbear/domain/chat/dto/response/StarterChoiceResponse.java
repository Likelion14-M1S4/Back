package com.meisterbear.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "대화 시작 선택지")
public class StarterChoiceResponse {

    @Schema(description = "선택지 식별자", example = "1")
    private Long id;

    @Schema(description = "선택지 문구", example = "제품이 오염됐어")
    private String label;

    @Schema(description = "선택 시 POST /api/chat/messages로 같이 보낼 태그. care면 이후 사진 업로드(인스펙터)로 이어짐",
            example = "care")
    private String tagName;
}
