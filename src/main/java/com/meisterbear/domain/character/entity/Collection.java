package com.meisterbear.domain.character.entity;

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

// 유저가 어떤 제품/캐릭터를 소유했는지 LOCKED→PREVIEW→OWNED 단계로 추적하는 기록
@Entity
@Getter
@Table(name = "collection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 캐릭터는 시즌×지역 템플릿이라 여러 유저가 공유 가능
    @Column(name = "character_id", nullable = false)
    private Long characterId;

    // 물리적 제품 1개를 여러 유저가 태그해도 각자 등록된다 (선점 없음)
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionStatus status;

    @Builder
    private Collection(Long userId, Long characterId, Long productId, CollectionStatus status) {
        this.userId = userId;
        this.characterId = characterId;
        this.productId = productId;
        this.status = status != null ? status : CollectionStatus.LOCKED;
    }

    public void preview() {
        if (this.status != CollectionStatus.LOCKED) {
            throw new IllegalStateException("LOCKED 상태에서만 PREVIEW로 전환할 수 있습니다.");
        }
        this.status = CollectionStatus.PREVIEW;
    }

    public void own() {
        if (this.status != CollectionStatus.PREVIEW) {
            throw new IllegalStateException("PREVIEW 상태에서만 OWNED로 전환할 수 있습니다.");
        }
        this.status = CollectionStatus.OWNED;
    }
}
