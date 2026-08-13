package com.meisterbear.domain.user.entity;

import com.meisterbear.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "refresh_token")
    private String refreshToken;

    private String email;

    private String phone;

    // 카카오는 생일(MMDD)과 출생연도를 별도로, 그것도 동의한 경우만 내려줘서 완전한 날짜가 아닐 수 있음 - 문자열로 보관
    @Column(name = "birth_date")
    private String birthDate;

    @Builder
    private User(Long kakaoId, String nickname, Role role, String email, String phone, String birthDate) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    // Refresh Token 재발급/로그인 시 갱신
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // 로그아웃/탈퇴 시 초기화
    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
