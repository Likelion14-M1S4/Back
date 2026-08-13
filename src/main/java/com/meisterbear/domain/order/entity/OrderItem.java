package com.meisterbear.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "order_no", nullable = false, length = 100)
    private String orderNo;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "paid_price", nullable = false)
    private Integer paidPrice;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(length = 100)
    private String seller;

    // 구 warranty.issued_at
    @Column(name = "warranty_issued_at")
    private LocalDate warrantyIssuedAt;

    // 구 warranty.expires_at
    @Column(name = "warranty_expires_at")
    private LocalDate warrantyExpiresAt;

    @Builder
    private OrderItem(Long userId, Long productId, Long storeId, String orderNo, Integer qty, Integer paidPrice,
            LocalDateTime orderedAt, String seller, LocalDate warrantyIssuedAt, LocalDate warrantyExpiresAt) {
        this.userId = userId;
        this.productId = productId;
        this.storeId = storeId;
        this.orderNo = orderNo;
        int resolvedQty = qty != null ? qty : 1;
        if (resolvedQty < 1) {
            throw new IllegalArgumentException("qty는 1 이상이어야 합니다.");
        }
        this.qty = resolvedQty;
        this.paidPrice = paidPrice;
        this.orderedAt = orderedAt;
        this.seller = seller;
        if (warrantyIssuedAt != null && warrantyExpiresAt != null && warrantyExpiresAt.isBefore(warrantyIssuedAt)) {
            throw new IllegalArgumentException("warrantyExpiresAt은 warrantyIssuedAt 이후여야 합니다.");
        }
        this.warrantyIssuedAt = warrantyIssuedAt;
        this.warrantyExpiresAt = warrantyExpiresAt;
    }

    public void markReceived() {
        if (this.receivedAt != null) {
            throw new IllegalStateException("이미 수령 처리된 주문입니다.");
        }
        this.receivedAt = LocalDateTime.now();
    }
}
