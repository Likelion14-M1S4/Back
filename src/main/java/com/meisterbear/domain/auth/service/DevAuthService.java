package com.meisterbear.domain.auth.service;

import com.meisterbear.domain.auth.dto.response.TokenResponse;
import com.meisterbear.domain.user.entity.Role;
import com.meisterbear.domain.user.entity.User;
import com.meisterbear.domain.user.exception.UserErrorCode;
import com.meisterbear.domain.user.repository.UserRepository;
import com.meisterbear.global.exception.CustomException;
import com.meisterbear.security.CustomUserDetails;
import com.meisterbear.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 개발/Swagger 테스트 전용. 카카오 없이 JWT를 즉시 발급한다.
@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevAuthService {

    // userId를 안 넘겼을 때 사용할 공용 테스트 유저 식별용 kakaoId (실제 카카오 id와 겹치지 않는 sentinel)
    private static final Long TEST_KAKAO_ID = 0L;
    private static final String TEST_NICKNAME = "테스트유저";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // userId를 주면 그 유저로 토큰 발급(없으면 404), 안 주면 공용 테스트 유저를 get-or-create 해서 발급한다
    @Transactional
    public TokenResponse issueTestToken(Long userId) {
        User user = (userId != null)
                ? userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND))
                : getOrCreateTestUser();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtProvider.createAccessToken(userDetails);
        String refreshToken = jwtProvider.createRefreshToken(userDetails);
        user.updateRefreshToken(refreshToken);

        log.info("[DevAuthService] 개발용 토큰 발급 - userId={}", user.getId());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private User getOrCreateTestUser() {
        return userRepository.findByKakaoId(TEST_KAKAO_ID)
                .orElseGet(() -> userRepository.save(User.builder()
                        .kakaoId(TEST_KAKAO_ID)
                        .nickname(TEST_NICKNAME)
                        .role(Role.USER)
                        .build()));
    }
}
