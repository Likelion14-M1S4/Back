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
@Table(name = "story")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false)
    private Long characterId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "unlock_order", nullable = false)
    private Integer unlockOrder;

    @Column(nullable = false, length = 50)
    private String season;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Builder
    private Story(Long characterId, String title, Integer unlockOrder, String season, String thumbnailUrl) {
        this.characterId = characterId;
        this.title = title;
        this.unlockOrder = unlockOrder != null ? unlockOrder : 0;
        this.season = season;
        this.thumbnailUrl = thumbnailUrl;
    }
}