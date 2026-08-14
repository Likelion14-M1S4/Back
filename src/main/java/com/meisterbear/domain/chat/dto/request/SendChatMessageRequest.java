package com.meisterbear.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "동행과의 대화 요청")
public class SendChatMessageRequest {

    @Schema(description = "대화 상대 character.id", example = "1")
    private Long characterId;

    @Schema(description = "유저 발화 (자유 입력 또는 선택지 label 그대로)", example = "이 제품에 대해 알려줘")
    private String message;

    @Schema(description = "진입 화면 선택지를 골랐다면 그 tagName. 자유 입력이면 생략", example = "product")
    private String tagName;

    @Valid
    @Size(max = 40, message = "history는 최대 40턴까지만 보낼 수 있습니다.")
    @Schema(description = "직전까지의 대화 내역. 대화 기록을 저장하지 않으므로 멀티턴 맥락이 필요하면 매번 같이 보낸다. "
            + "첫 메시지면 생략 가능",
            example = "[{\"role\":\"USER\",\"content\":\"안녕\"},"
                    + "{\"role\":\"CHARACTER\",\"content\":\"안녕하세요, 박세은님. 어떤 얘기를 나눠볼까요?\"}]")
    private List<ChatHistoryMessage> history;
}
