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

    @Column(unique = true)
    private String email;

    private String phone;

    // 카카오는 생일/출생연도를 동의 시에만 별도로 내려줘서 완전한 날짜가 아닐 수 있어 문자열로 보관
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

    // 마이페이지 - 이메일은 카카오 연동 값이라 여기서 수정하지 않음
    public void updateProfile(String nickname, String phone, String birthDate) {
        this.nickname = nickname;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    // 로그아웃/탈퇴 시 초기화
    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
