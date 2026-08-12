package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 상세 조회 응답")
public class StoryDetailResponse {

    @Schema(description = "story.id", example = "1")
    private Long id;

    @Schema(description = "story.title", example = "챕터 1 - 루나의 시작")
    private String title;

    @Schema(description = "story.unlock_order", example = "1")
    private Integer unlockOrder;

    // Lombok의 isLocked() getter를 Jackson이 "locked"로 직렬화하는 것을 방지
    @JsonProperty("isLocked")
    @Schema(description = "story.is_locked", example = "false")
    private boolean isLocked;

    @JsonProperty("isDone")
    @Schema(description = "user_story_progress.is_done", example = "true")
    private boolean isDone;

    @Schema(description = "장면 목록 (sceneOrder 순 정렬)")
    private List<StorySceneResponse> scenes;

    @Schema(description = "질문·선택지 목록")
    private List<StoryQuestionResponse> questions;
}
