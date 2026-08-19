package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참 상세 조회 응답. 구매 가능 여부는 스토리 상세 조회 응답의 isSeasonCompleted 값으로 프론트에서 판단한다")
public class CharmDetailResponse {

    @Schema(description = "charm.id", example = "1")
    private Long id;

    @Schema(description = "charm.character_id - 이 참에 대응하는 캐릭터 id. 채팅 진입(GET /api/chat/{characterId}/entry)에 사용",
            example = "1")
    private Long characterId;

    @Schema(description = "charm.name", example = "MCM 비세토스 라이언 참")
    private String name;

    @Schema(description = "charm.price", example = "410000")
    private Integer price;

    @Schema(description = "charm.color", example = "꼬냑")
    private String color;

    @Schema(description = "charm.img_url", example = "https://cdn.meisterbear.com/charm/1.png")
    private String imgUrl;

    @Schema(description = "charm.description", example = "MCM의 상징성을 담아낸 라이언 참은 시그니처 비세토스 패턴과 정교한 가죽 디테일을 조화롭게 담아낸 아이코닉 액세서리입니다.")
    private String description;

    @Schema(description = "charm.collection_name", example = "MCM BASIC COLLECTION")
    private String collectionName;
}
