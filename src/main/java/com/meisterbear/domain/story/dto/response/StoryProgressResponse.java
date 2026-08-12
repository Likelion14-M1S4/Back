package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터별 진행 상태")
public class StoryProgressResponse {

    @Schema(description = "story.id", example = "1")
    private Long id;

    @Schema(description = "story.title", example = "챕터 1 - 루나의 시작")
    private String title;

    @Schema(description = "story.unlock_order", example = "1")
    private Integer unlockOrder;

    @JsonProperty("isLocked")
    @Schema(description = "story.is_locked", example = "false")
    private boolean isLocked;

    @JsonProperty("isDone")
    @Schema(description = "user_story_progress.is_done", example = "true")
    private boolean isDone;

    @Schema(description = "user_story_progress.read_at", example = "2026-08-10T11:00:00")
    private LocalDateTime readAt;
}
