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

    @Builder
    private Charm(Long storeId, String name, String imgUrl, String description, String tagName,
                  String collectionName, Integer price, String color) {
        this.storeId = storeId;
        this.name = name;
        this.imgUrl = imgUrl;
        this.description = description;
        this.tagName = tagName;
        this.collectionName = collectionName;
        this.price = price;
        this.color = color;
    }
}
