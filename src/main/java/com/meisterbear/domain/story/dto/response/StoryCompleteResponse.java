package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 완주 처리 응답")
public class StoryCompleteResponse {

    @Schema(description = "현재 챕터 완주 여부", example = "true")
    private boolean isDone;

    @Schema(description = "3챕터 전체 완주 여부", example = "true")
    private boolean isAllCompleted;

    @Schema(description = "다음 챕터 ID (전체 완주 시 null)", example = "2")
    private Long nextStoryId;

    @Schema(description = "product.id (스토리가 속한 캐릭터의 제품)", example = "1")
    private Long productId;
}
