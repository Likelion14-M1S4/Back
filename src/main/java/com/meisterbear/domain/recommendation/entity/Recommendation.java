package com.meisterbear.domain.recommendation.entity;

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
@Table(name = "recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 제품 추천 시 사용
    @Column(name = "product_id")
    private Long productId;

    // 참 추천 시 사용
    @Column(name = "charm_id")
    private Long charmId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // today: 오늘의 추천, charm_match: AI 고객 취향 기반 추천
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Recommendation(Long userId, Long productId, Long charmId, String reason, RecommendationType type) {
        if ((productId == null) == (charmId == null)) {
            throw new IllegalArgumentException("productId와 charmId 중 정확히 하나만 지정해야 합니다.");
        }
        this.userId = userId;
        this.productId = productId;
        this.charmId = charmId;
        this.reason = reason;
        this.type = type;
    }
}