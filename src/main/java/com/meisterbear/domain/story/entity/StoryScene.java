package com.meisterbear.domain.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "story_scene")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "scene_order", nullable = false)
    private Integer sceneOrder;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    private StoryScene(Long storyId, Integer sceneOrder, String imgUrl, String content) {
        this.storyId = storyId;
        this.sceneOrder = sceneOrder;
        this.imgUrl = imgUrl;
        this.content = content;
    }
}