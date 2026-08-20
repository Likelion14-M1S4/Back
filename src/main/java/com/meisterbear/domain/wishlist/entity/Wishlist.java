package com.meisterbear.domain.wishlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

// NULL은 유니크 인덱스에서 서로 다른 값 취급이라, 두 제약은 각각 제품/참 찜 중복만 막는다
@Entity
@Getter
@Table(name = "wishlist",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_WISHLIST_USER_PRODUCT", columnNames = {"user_id", "product_id"}),
                @UniqueConstraint(name = "UQ_WISHLIST_USER_CHARM", columnNames = {"user_id", "charm_id"})
        })
@Check(constraints = "(product_id IS NULL) <> (charm_id IS NULL)")
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
