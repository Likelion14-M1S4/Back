package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 완료 결과")
public class StoryCompleteResultResponse {

    @Schema(description = "story.id", example = "3")
    private Long storyId;

    @JsonProperty("isDone")
    @Schema(description = "user_story_progress.is_done", example = "true")
    private boolean isDone;

    @Schema(description = "user_story_progress.read_at", example = "2026-08-13T13:20:00")
    private LocalDateTime readAt;

    @JsonProperty("isSeasonCompleted")
    @Schema(description = "이 챕터가 속한 시즌의 전체 챕터를 이 유저가 완주했는지 여부 (true면 프론트에서 시즌 완료 화면 노출)",
            example = "false")
    private boolean isSeasonCompleted;

    @Schema(description = "character.name (시즌 완료 화면 문구에 사용, isSeasonCompleted=true일 때만 의미 있음)",
            example = "비세토스 라이언")
    private String characterName;

    @Schema(description = "character.img_url (시즌 완료 화면에 사용, isSeasonCompleted=true일 때만 의미 있음)",
            example = "https://cdn.meisterbear.com/character/1.png")
    private String characterImgUrl;
}
