package com.meisterbear.security;

import com.meisterbear.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    // 카카오 소셜 로그인 - 비밀번호 미사용
    @Override
    public String getPassword() {
        return null;
    }

    // 사용자 식별값으로 kakaoId 사용
    @Override
    public String getUsername() {
        return String.valueOf(user.getKakaoId());
    }
}
