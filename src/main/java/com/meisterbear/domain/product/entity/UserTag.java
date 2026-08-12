package com.meisterbear.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false)
    private TagType tagType;

    @Column(name = "tag_name", length = 255)
    private String tagName;

    @Builder
    private UserTag(Long userId, Long productId, TagType tagType, String tagName) {
        this.userId = userId;
        this.productId = productId;
        this.tagType = tagType;
        this.tagName = tagName;
    }
}