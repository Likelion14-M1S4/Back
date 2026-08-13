package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "현재 시즌 스토리")
public class CurrentSeasonResponse {

    @Schema(description = "현재 시즌 코드", example = "AW2026")
    private String season;

    @Schema(description = "챕터 목록 (unlockOrder 순 정렬)")
    private List<StoryProgressResponse> stories;

    @JsonProperty("isAllCompleted")
    @Schema(description = "이 시즌 전체 챕터 완주 여부", example = "false")
    private boolean isAllCompleted;
}
