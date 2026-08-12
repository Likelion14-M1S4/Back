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

    @Column(name = "character_id", nullable = false, unique = true)
    private Long characterId;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionStatus status;

    @Builder
    private Collection(Long userId, Long characterId, CollectionStatus status) {
        this.userId = userId;
        this.characterId = characterId;
        this.status = status != null ? status : CollectionStatus.LOCKED;
    }

    // 구매 직후 - 컬렉션 추가 가능 상태로 전환
    public void preview() {
        this.status = CollectionStatus.PREVIEW;
    }

    // 컬렉션 추가 버튼 클릭 - 소유 확정
    public void own() {
        this.status = CollectionStatus.OWNED;
    }
}