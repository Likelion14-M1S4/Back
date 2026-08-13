package com.meisterbear.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_store",
        uniqueConstraints = @UniqueConstraint(name = "UQ_PRODUCT_STORE", columnNames = {"product_id", "store_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Builder
    private ProductStore(Long productId, Long storeId) {
        this.productId = productId;
        this.storeId = storeId;
    }
}
