package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참에 연결된 캐릭터 정보 (일반 참일 때만 채워짐, 시즌 한정 참이면 null)")
public class CharmCharacterResponse {

    @Schema(description = "character.id", example = "3")
    private Long id;

    @Schema(description = "character.name", example = "비세토스 라이언")
    private String name;

    @Schema(description = "character.personality. 참 상세 화면 상단에 바로 노출되는 캐릭터 소개 문구",
            example = "항상 침착하고 여유로운 태도를 유지하며, 화려하게 자신을 드러내기보다 자연스럽게 존재감을 보여줍니다.")
    private String personality;

    @Schema(description = "character.intro. '캐릭터' 아코디언을 펼쳤을 때 노출되는 설명",
            example = "독일 뮌헨의 정신을 이어받은 MCM의 상징적인 라이언 캐릭터입니다.")
    private String intro;
}
