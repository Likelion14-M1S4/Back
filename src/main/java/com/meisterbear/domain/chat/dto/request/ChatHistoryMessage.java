package com.meisterbear.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이전 대화 한 턴")
public class ChatHistoryMessage {

    @NotBlank
    @Pattern(regexp = "USER|CHARACTER", message = "role은 USER 또는 CHARACTER여야 합니다.")
    @Schema(description = "발화자. USER 또는 CHARACTER", example = "USER")
    private String role;

    @NotBlank
    @Size(max = 2000, message = "content는 2000자를 넘을 수 없습니다.")
    @Schema(description = "발화 내용", example = "안녕")
    private String content;
}
