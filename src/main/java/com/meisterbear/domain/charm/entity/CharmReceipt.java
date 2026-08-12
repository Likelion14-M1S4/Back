package com.meisterbear.domain.charm.entity;

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
@Table(name = "charm_receipt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharmReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "charm_id", nullable = false)
    private Long charmId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 4)
    private String receiptNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CharmReceiptStatus status;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "tagged_at", nullable = false, updatable = false)
    private LocalDateTime taggedAt;

    // 예: AW2026
    @Column(nullable = false, length = 50)
    private String season;

    @Builder
    private CharmReceipt(Long charmId, Long userId, Long productId, String receiptNo, String season) {
        if (receiptNo == null || !receiptNo.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("receiptNo는 숫자 4자리여야 합니다.");
        }
        this.charmId = charmId;
        this.userId = userId;
        this.productId = productId;
        this.receiptNo = receiptNo;
        this.status = CharmReceiptStatus.SELECTED;
        this.season = season;
    }

    public void complete() {
        this.status = CharmReceiptStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    // 탈퇴 시 계정 연결만 해제, 제품·시즌 수령 기록은 유지
    public void detachUser() {
        this.userId = null;
    }
}