package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 탭 조회 응답")
public class StoryListResponse {

    @Schema(description = "시즌 코드 (등록된 제품이 없거나 스토리가 없으면 null)", example = "AW2026")
    private String season;

    @Schema(description = "챕터 목록 (unlockOrder 순 정렬, 등록된 제품이 없거나 스토리가 없으면 빈 배열)",
            example = "[{\"id\":1,\"title\":\"Introduction\",\"unlockOrder\":1,\"isLocked\":false,"
                    + "\"isDone\":true,\"readAt\":\"2026-08-10T11:00:00\",\"teaser\":null,"
                    + "\"thumbnailUrl\":\"https://cdn.meisterbear.com/story/1-thumb.png\"}]")
    @Builder.Default
    private List<StoryProgressResponse> stories = List.of();

    @JsonProperty("isAllCompleted")
    @Schema(description = "전체 챕터 완주 여부", example = "false")
    private boolean allCompleted;

    public static StoryListResponse empty() {
        return StoryListResponse.builder().build();
    }
}
