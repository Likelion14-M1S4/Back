package com.meisterbear.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이전 대화 한 턴")
public class ChatHistoryMessage {

    @Schema(description = "발화자. USER 또는 CHARACTER", example = "USER")
    private String role;

    @Schema(description = "발화 내용", example = "안녕")
    private String content;
}
