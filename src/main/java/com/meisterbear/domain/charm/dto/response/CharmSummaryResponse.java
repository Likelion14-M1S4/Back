package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참 목록의 카드 하나")
public class CharmSummaryResponse {

    @Schema(description = "charm.id", example = "1")
    private Long id;

    @Schema(description = "charm.character_id - 이 참에 대응하는 캐릭터 id. 채팅 진입(GET /api/chat/{characterId}/entry)에 사용",
            example = "1")
    private Long characterId;

    @Schema(description = "charm.name", example = "비세토스 라이언")
    private String name;

    @Schema(description = "charm.img_url", example = "https://cdn.meisterbear.com/charm/1.png")
    private String imgUrl;

    @Schema(description = "charm.collection_name", example = "MCM BASIC COLLECTION")
    private String collectionName;
}
