package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 탭 조회 응답")
public class StoryListResponse {

    @Schema(description = "현재 시즌 스토리 (등록된 제품이 없으면 null)")
    private CurrentSeasonResponse currentSeason;

    @Schema(description = "지난 시즌 목록")
    private List<PastSeasonResponse> pastSeasons;

    public static StoryListResponse empty() {
        return StoryListResponse.builder()
                .currentSeason(null)
                .pastSeasons(List.of())
                .build();
    }
}
