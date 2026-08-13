package com.meisterbear.domain.story.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 상세 조회 응답")
public class StoryDetailResponse {

    @Schema(description = "story.id", example = "3")
    private Long id;

    @Schema(description = "story.title", example = "Craftmanship")
    private String title;

    @Schema(description = "story.unlock_order", example = "3")
    private Integer unlockOrder;

    @JsonProperty("isDone")
    @Schema(description = "user_story_progress.is_done", example = "false")
    private boolean isDone;

    @Schema(description = "user_story_progress.read_at", example = "2026-08-13T13:20:00")
    private LocalDateTime readAt;

    @Schema(description = "장면 목록 (order 순 정렬)")
    private List<SceneResponse> scenes;

    @Schema(description = "챕터 마지막 질문 (없으면 null)")
    private QuestionResponse question;

    @JsonProperty("isSeasonCompleted")
    @Schema(description = "이 챕터가 속한 시즌의 전체 챕터를 이 유저가 완주했는지 여부 (true면 프론트에서 시즌 완료 화면 노출)",
            example = "false")
    private boolean isSeasonCompleted;

    @Schema(description = "character.name (시즌 완료 화면 문구에 사용)", example = "비세토스 라이언")
    private String characterName;

    @Schema(description = "character.img_url (시즌 완료 화면에 사용)", example = "https://cdn.meisterbear.com/character/1.png")
    private String characterImgUrl;
}
