package com.meisterbear.domain.charm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "참 상세 조회 응답. isSeasonLimited 값으로 화면을 분기한다 "
        + "(true: character는 null, isPurchasable로 구매 버튼 상태 결정 / false: isPurchasable은 null, character로 캐릭터 정보+대화하기 버튼 노출)")
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

    @JsonProperty("isSeasonLimited")
    @Schema(description = "charm.is_season_limited. 프론트 화면 분기 기준값 "
            + "(true면 구매 가능 여부 버튼 UI, false면 캐릭터 정보 + '캐릭터와 대화하기' 버튼 UI)", example = "true")
    private boolean seasonLimited;

    @JsonProperty("isPurchasable")
    @Schema(description = "구매 가능 여부. isSeasonLimited=true일 때만 값이 채워짐(false면 항상 null). "
            + "이 참에 연결된 캐릭터×시즌 스토리를 이 유저가 전부 완주했는지로 판단 "
            + "(false면 프론트에서 '스토리 진행 후 구매 가능' 버튼, true면 '구매 가능' 버튼 노출)", example = "false")
    private Boolean purchasable;

    @Schema(description = "isSeasonLimited=false일 때만 값이 채워짐(true면 항상 null). 참에 연결된 캐릭터 정보")
    private CharmCharacterResponse character;
}
