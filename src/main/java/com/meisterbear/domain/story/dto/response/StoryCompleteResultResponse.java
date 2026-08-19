package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
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
    private boolean done;

    @Schema(description = "user_story_progress.read_at", example = "2026-08-13T13:20:00")
    private LocalDateTime readAt;

    @JsonProperty("isSeasonCompleted")
    @Schema(description = "이 챕터가 속한 시즌의 전체 챕터를 이 유저가 완주했는지 여부 (true면 프론트에서 시즌 완료 화면 노출)",
            example = "false")
    private boolean seasonCompleted;

    @Schema(description = "이 시즌 완료로 받는 참 목록 (isSeasonCompleted=true일 때만 채워짐)",
            example = "[{\"id\":1,\"name\":\"라이언 참\",\"imgUrl\":\"https://cdn.meisterbear.com/charm/1.png\","
                    + "\"collectionName\":\"라이언 컬렉션\"}]")
    @Builder.Default
    private List<RewardCharmResponse> charms = List.of();
}
