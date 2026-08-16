package com.meisterbear.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    // STORE 태그가 어느 매장에서 발생했는지. 매장 태그 이력을 매장별로 묶는 키
    // (PURCHASE 태그 등 매장과 무관한 태그는 null 허용)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "tag_name", length = 255)
    private String tagName;

    // 매장 태그 이력 날짜별 그룹핑용
    @CreationTimestamp
    @Column(name = "tagged_at", nullable = false, updatable = false)
    private LocalDateTime taggedAt;

    @Builder
    private UserTag(Long userId, Long productId, TagType tagType, String tagName, Long storeId) {
        this.userId = userId;
        this.productId = productId;
        this.tagType = tagType;
        this.tagName = tagName;
        this.storeId = storeId;
    }
}
