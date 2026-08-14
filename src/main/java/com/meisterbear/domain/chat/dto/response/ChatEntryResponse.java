package com.meisterbear.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "채팅 진입 화면 조회 응답")
public class ChatEntryResponse {

    @Schema(description = "character.id (이후 /messages, /inspector 호출 시 그대로 실어 보냄)", example = "1")
    private Long characterId;

    @Schema(description = "character.name", example = "비세토스 라이언")
    private String characterName;

    @Schema(description = "character.img_url", example = "https://cdn.meisterbear.com/character/1.png")
    private String characterImgUrl;

    @Schema(description = "유저 닉네임이 반영된 인사말", example = "안녕하세요, 박세은님. 어떤 얘기를 나눠볼까요?")
    private String greeting;

    @Schema(description = "대화 시작 선택지 3개 (고정값)")
    private List<StarterChoiceResponse> starterChoices;
}
