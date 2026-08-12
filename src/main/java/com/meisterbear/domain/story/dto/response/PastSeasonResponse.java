package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "지난 시즌")
public class PastSeasonResponse {

    @Schema(description = "story.season", example = "SS2026")
    private String season;

    @Schema(description = "story.thumbnail_url", example = "https://...")
    private String thumbnailUrl;
}
