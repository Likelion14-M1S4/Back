package com.meisterbear.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "warranty")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Warranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "serial_no", nullable = false, length = 100)
    private String serialNo;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Builder
    private Warranty(Long orderItemId, String serialNo, LocalDate issuedAt, LocalDate expiresAt) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("issuedAt은 필수입니다.");
        }
        if (expiresAt != null && expiresAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("expiresAt은 issuedAt 이후여야 합니다.");
        }
        this.orderItemId = orderItemId;
        this.serialNo = serialNo;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}