package com.meisterbear.domain.story.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "스토리 장면")
public class StorySceneResponse {

    @Schema(description = "story_scene.id", example = "1")
    private Long id;

    @Schema(description = "story_scene.scene_order", example = "1")
    private Integer sceneOrder;

    @Schema(description = "story_scene.img_url", example = "https://...")
    private String imgUrl;

    @Schema(description = "story_scene.content", example = "루나가 등장합니다...")
    private String content;
}
