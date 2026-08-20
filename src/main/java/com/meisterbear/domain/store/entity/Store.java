package com.meisterbear.domain.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "store")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    // 요일별 운영시간 JSON: [{"day":"월요일","time":"10:30 - 20:00"}, ...]
    @Column(columnDefinition = "TEXT")
    private String hours;

    @Builder
    private Store(String name, String address, String phone, String postalCode, String hours) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.postalCode = postalCode;
        this.hours = hours;
    }
}
