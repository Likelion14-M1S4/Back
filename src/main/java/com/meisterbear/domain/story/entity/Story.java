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

    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    @Column(name = "unlock_order", nullable = false)
    private Integer unlockOrder;

    @Column(nullable = false, length = 50)
    private String season;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    // 장면 목록 JSON. 예: [{"id":1,"order":1,"imgUrl":"...","content":"..."}, ...]
    @Column(columnDefinition = "json")
    private String scenes;

    @Builder
    private Story(Long characterId, String title, Boolean locked, Integer unlockOrder, String season,
                  String thumbnailUrl, String scenes) {
        this.characterId = characterId;
        this.title = title;
        this.locked = locked != null ? locked : true;
        this.unlockOrder = unlockOrder != null ? unlockOrder : 0;
        this.season = season;
        this.thumbnailUrl = thumbnailUrl;
        this.scenes = scenes;
    }

    // 순서(1→2→3)에 따라 다음 챕터 해금
    public void unlock() {
        this.locked = false;
    }
}