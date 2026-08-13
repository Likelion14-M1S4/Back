package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "챕터 질문 선택 결과")
public class StoryChoiceResultResponse {

    @Schema(description = "story.id", example = "3")
    private Long storyId;

    @Schema(description = "선택한 choice의 tagName", example = "classic")
    private String tagName;

    @Schema(description = "선택한 choice에 연결된 다음 장면 목록 (order 순 정렬). 프론트는 이 장면들을 이어서 재생한다",
            example = "[{\"order\":1,\"imgUrl\":\"https://cdn.meisterbear.com/story/3-classic-1.png\","
                    + "\"content\":\"이번 시즌 클래식 라인업을 소개할게요.\"}]")
    private List<SceneResponse> scenes;
}
