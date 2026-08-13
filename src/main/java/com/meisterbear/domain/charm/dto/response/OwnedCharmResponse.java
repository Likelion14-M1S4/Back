package com.meisterbear.domain.charm.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "보유한 참")
public class OwnedCharmResponse {

    @Schema(description = "charm.id", example = "1")
    private Long id;

    @Schema(description = "charm.name", example = "비세토스 라이언")
    private String name;

    @Schema(description = "charm.img_url", example = "https://cdn.meisterbear.com/charm/1.png")
    private String imgUrl;

    @Schema(description = "charm.collection_name", example = "MCM BASIC COLLECTION")
    private String collectionName;
}
