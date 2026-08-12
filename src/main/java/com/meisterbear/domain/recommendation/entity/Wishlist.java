package com.meisterbear.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "wishlist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // product 찜 시 사용
    @Column(name = "product_id")
    private Long productId;

    // charm 찜 시 사용
    @Column(name = "charm_id")
    private Long charmId;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt;

    @Builder
    private Wishlist(Long userId, Long productId, Long charmId) {
        if ((productId == null) == (charmId == null)) {
            throw new IllegalArgumentException("productId와 charmId 중 정확히 하나만 지정해야 합니다.");
        }
        this.userId = userId;
        this.productId = productId;
        this.charmId = charmId;
    }
}