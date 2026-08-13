package com.meisterbear.domain.charm.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "시즌 한정 참 상세 조회 응답")
public class CharmDetailResponse {

    @Schema(description = "charm.id", example = "1")
    private Long id;

    @Schema(description = "charm.name", example = "MCM 비세토스 라이언 참")
    private String name;

    @Schema(description = "charm.price", example = "410000")
    private Integer price;

    @Schema(description = "charm.color", example = "꼬냑")
    private String color;

    @Schema(description = "charm.img_url", example = "https://cdn.meisterbear.com/charm/1.png")
    private String imgUrl;

    @JsonProperty("isPurchasable")
    @Schema(description = "구매 가능 여부. 이 유저가 현재 시즌 스토리를 전부 완주했는지로 판단 "
            + "(false면 프론트에서 '스토리 진행 후 구매 가능' 버튼 노출)", example = "false")
    private boolean isPurchasable;
}
