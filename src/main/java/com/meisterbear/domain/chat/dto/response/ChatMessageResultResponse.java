package com.meisterbear.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "동행과의 대화 결과")
public class ChatMessageResultResponse {

    @Schema(description = "응답한 character.id", example = "1")
    private Long characterId;

    @Schema(description = "캐릭터 페르소나 톤으로 생성된 답변",
            example = "이번 시즌 비세토스 라이언 백팩은 최상급 새들 레더로 만들어졌어요. 궁금한 게 더 있나요?")
    private String reply;
}
