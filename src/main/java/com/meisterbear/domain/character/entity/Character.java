package com.meisterbear.domain.character.entity;

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
@Table(name = "`character`")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제품 하나=캐릭터 하나 - 같은 제품에 캐릭터가 여러 개 생기는 걸 막기 위해 unique
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(columnDefinition = "TEXT")
    private String personality;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(name = "locked_img_url", length = 255)
    private String lockedImgUrl;

    @Builder
    private Character(Long productId, String name, String imgUrl, String personality, String intro,
            String lockedImgUrl) {
        this.productId = productId;
        this.name = name;
        this.imgUrl = imgUrl;
        this.personality = personality;
        this.intro = intro;
        this.lockedImgUrl = lockedImgUrl;
    }
}
