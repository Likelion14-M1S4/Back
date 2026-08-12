package com.meisterbear.domain.product.entity;

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
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String series;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "serial_no", nullable = false, unique = true, length = 100)
    private String serialNo;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String season;

    @Column(length = 100)
    private String material;

    @Column(length = 100)
    private String atelier;

    @Column(length = 50)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(nullable = false, length = 50)
    private String color;

    @Column(nullable = false, length = 50)
    private String size;

    @Column(length = 50)
    private String category;

    @Column(name = "nfc_uid", unique = true, length = 50)
    private String nfcUid;

    @Builder
    private Product(String name, String series, Integer price, String serialNo, String detail, String imgUrl,
                    String season, String material, String atelier, String region, ProductStatus status,
                    String color, String size, String category, String nfcUid) {
        this.name = name;
        this.series = series;
        this.price = price;
        this.serialNo = serialNo;
        this.detail = detail;
        this.imgUrl = imgUrl;
        this.season = season;
        this.material = material;
        this.atelier = atelier;
        this.region = region;
        this.status = status;
        this.color = color;
        this.size = size;
        this.category = category;
        this.nfcUid = nfcUid;
    }

    // 직원 판매 처리 - 진열품을 판매됨으로 전환
    public void markSold() {
        this.status = ProductStatus.SOLD;
        this.soldAt = LocalDateTime.now();
    }
}