package com.meisterbear.domain.charm.entity;

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
@Table(name = "charm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Charm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    // 이 참을 받으려면 완주해야 하는 시즌 스토리의 캐릭터. 캐릭터=참 1:1이라 unique
    @Column(name = "character_id", nullable = false, unique = true)
    private Long characterId;

    // 예: AW2026
    @Column(nullable = false, length = 50)
    private String season;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tag_name", length = 100)
    private String tagName;

    @Column(name = "collection_name", length = 100)
    private String collectionName;

    private Integer price;

    @Column(length = 100)
    private String color;

    // true면 isPurchasable, false면 character 정보를 채워서 응답
    @Column(name = "is_season_limited", nullable = false)
    private boolean seasonLimited;

    @Builder
    private Charm(Long storeId, Long characterId, String season, String name, String imgUrl, String description,
                  String tagName, String collectionName, Integer price, String color, boolean seasonLimited) {
        this.storeId = storeId;
        this.characterId = characterId;
        this.season = season;
        this.name = name;
        this.imgUrl = imgUrl;
        this.description = description;
        this.tagName = tagName;
        this.collectionName = collectionName;
        this.price = price;
        this.color = color;
        this.seasonLimited = seasonLimited;
    }

    // 이름만 캐릭터 값으로 맞춘다 (이미지는 동기화 안 함)
    public void syncName(String characterName) {
        this.name = characterName;
    }
}
