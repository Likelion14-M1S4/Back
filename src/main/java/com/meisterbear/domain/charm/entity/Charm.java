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

    // 이 참을 받으려면 완주해야 하는 시즌 스토리의 캐릭터 (character는 시즌×지역 템플릿)
    // 캐릭터=참 1:1 - 캐릭터당 참이 여러 개 생기는 걸 막기 위해 unique
    @Column(name = "character_id", nullable = false, unique = true)
    private Long characterId;

    // 예: AW2026. character_id와 함께 story.character_id + story.season 조회에 사용
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

    // true면 상세 조회 시 isPurchasable(구매 가능 여부)을, false면 character(캐릭터 정보)를 채워서 응답
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

    // 캐릭터=참 1:1이라 표시 정보(이름/이미지)는 캐릭터 쪽을 원본으로 삼는다.
    // 캐릭터 수집(collect) 시점에 이 참의 표시 정보를 캐릭터 값으로 맞춰 드리프트를 방지한다.
    // 이미지는 참 자체 값을 유지한다(동기화 대상 아님) - 이름만 캐릭터 값으로 맞춘다
    public void syncName(String characterName) {
        this.name = characterName;
    }
}
