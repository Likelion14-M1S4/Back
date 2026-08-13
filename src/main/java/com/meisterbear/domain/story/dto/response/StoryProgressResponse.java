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

    @Schema(description = "story.title", example = "Introduction")
    private String title;

    @Schema(description = "story.unlock_order", example = "1")
    private Integer unlockOrder;

    @JsonProperty("isLocked")
    @Schema(description = "이 유저 기준 잠금 여부 (직전 챕터 완주 여부로 판단)", example = "false")
    private boolean isLocked;

    @JsonProperty("isDone")
    @Schema(description = "user_story_progress.is_done", example = "true")
    private boolean isDone;

    @Schema(description = "user_story_progress.read_at", example = "2026-08-10T11:00:00")
    private LocalDateTime readAt;

    @Schema(description = "미완주 챕터의 짧은 소개 문구 (story.scenes 첫 장면에서 추출, 완주했으면 null)",
            example = "제품을 만든 장인과 공방 속의 이야기를 들여다봅니다.")
    private String teaser;

    @Schema(description = "story.thumbnail_url", example = "https://cdn.meisterbear.com/story/3-thumb.png")
    private String thumbnailUrl;
}
