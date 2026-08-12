package com.meisterbear.domain.order.entity;

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

    @Builder
    private OrderItem(Long userId, Long productId, Long storeId, String orderNo, Integer qty, Integer paidPrice,
                      LocalDateTime orderedAt, String seller) {
        this.userId = userId;
        this.productId = productId;
        this.storeId = storeId;
        this.orderNo = orderNo;
        this.qty = qty != null ? qty : 1;
        this.paidPrice = paidPrice;
        this.orderedAt = orderedAt;
        this.seller = seller;
    }

    // 제품 수령 처리
    public void markReceived() {
        this.receivedAt = LocalDateTime.now();
    }
}