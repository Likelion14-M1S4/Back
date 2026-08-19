package com.meisterbear.domain.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "캐릭터 컬렉션 추가 응답")
public class CollectCharacterResponse {

    @Schema(description = "캐릭터 id", example = "1")
    private Long id;

    @Schema(description = "이 캐릭터에 대응하는 charm.id (매칭되는 참이 없으면 null)", example = "1")
    private Long charmId;

    @Schema(description = "수집 완료 여부 (이미 수집한 경우에도 true - 멱등)", example = "true")
    private Boolean collected;
}
