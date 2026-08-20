package com.meisterbear.domain.auth.service;

import com.meisterbear.domain.auth.client.KakaoClient;
import com.meisterbear.domain.auth.client.KakaoTokenResponse;
import com.meisterbear.domain.auth.client.KakaoUserResponse;
import com.meisterbear.domain.auth.dto.response.LoginResponse;
import com.meisterbear.domain.auth.dto.response.TokenResponse;
import com.meisterbear.domain.auth.exception.AuthErrorCode;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.charm.entity.CharmReceipt;
import com.meisterbear.domain.charm.repository.CharmReceiptRepository;
import com.meisterbear.domain.order.repository.OrderItemRepository;
import com.meisterbear.domain.product.repository.UserTagRepository;
import com.meisterbear.domain.story.repository.UserStoryProgressRepository;
import com.meisterbear.domain.user.entity.Role;
import com.meisterbear.domain.user.entity.User;
import com.meisterbear.domain.user.repository.UserRepository;
import com.meisterbear.domain.wishlist.repository.WishlistRepository;
import com.meisterbear.global.exception.CustomException;
import com.meisterbear.security.CustomUserDetails;
import com.meisterbear.security.JwtProvider;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    // 카카오 닉네임 미동의 시 사용할 기본값 (nickname 컬럼 NOT NULL)
    private static final String DEFAULT_NICKNAME = "사용자";

    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // 탈퇴 시 유저 연관 데이터 정리용
    private final CollectionRepository collectionRepository;
    private final UserStoryProgressRepository userStoryProgressRepository;
    private final UserTagRepository userTagRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderItemRepository orderItemRepository;
    private final CharmReceiptRepository charmReceiptRepository;

    // 최초 로그인이면 회원 생성, 기존 회원이면 refresh를 rotate한다
    @Transactional
    public LoginResponse kakaoLogin(String code, String redirectUri) {
        String kakaoAccessToken = exchangeCodeForToken(code, redirectUri);
        KakaoUserResponse kakaoUser = fetchKakaoUser(kakaoAccessToken);

        Optional<User> found = userRepository.findByKakaoId(kakaoUser.id());
        boolean isNewUser = found.isEmpty();
        User user = found.orElseGet(() -> userRepository.save(User.builder()
                .kakaoId(kakaoUser.id())
                .nickname(resolveNickname(kakaoUser))
                .email(resolveEmail(kakaoUser))
                .role(Role.USER)
                .build()));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtProvider.createAccessToken(userDetails);
        String refreshToken = jwtProvider.createRefreshToken(userDetails);
        user.updateRefreshToken(refreshToken);

        log.info("[AuthService] 카카오 로그인 완료 - userId={}, isNewUser={}", user.getId(), isNewUser);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .build();
    }

    // DB 저장값과 일치할 때만 재발급 (단일 세션)
    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = userRepository.findById(jwtProvider.getUserId(refreshToken))
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtProvider.createAccessToken(userDetails);
        String newRefreshToken = jwtProvider.createRefreshToken(userDetails);
        user.updateRefreshToken(newRefreshToken);

        log.info("[AuthService] 토큰 재발급 완료 - userId={}", user.getId());
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.UNAUTHORIZED));
        user.clearRefreshToken();
        log.info("[AuthService] 로그아웃 완료 - userId={}", userId);
    }

    // 하드 삭제. charm_receipt는 매장측 기록이라 연결만 해제하고 보존한다.
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.UNAUTHORIZED));

        collectionRepository.deleteByUserId(userId);
        userStoryProgressRepository.deleteByUserId(userId);
        userTagRepository.deleteByUserId(userId);
        wishlistRepository.deleteByUserId(userId);
        orderItemRepository.deleteByUserId(userId);

        List<CharmReceipt> receipts = charmReceiptRepository.findByUserId(userId);
        receipts.forEach(CharmReceipt::detachUser);

        userRepository.delete(user);
        log.info("[AuthService] 회원 탈퇴 완료 - userId={}", userId);
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        KakaoTokenResponse token;
        try {
            token = kakaoClient.getToken(code, redirectUri);
        } catch (HttpClientErrorException e) {
            log.warn("[AuthService] 카카오 인가 코드 교환 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(AuthErrorCode.INVALID_KAKAO_CODE);
        } catch (RestClientException e) {
            log.warn("[AuthService] 카카오 서버 통신 실패 - {}", e.getMessage());
            throw new CustomException(AuthErrorCode.KAKAO_SERVER_ERROR);
        }
        if (token == null || token.accessToken() == null) {
            throw new CustomException(AuthErrorCode.INVALID_KAKAO_CODE);
        }
        return token.accessToken();
    }

    private KakaoUserResponse fetchKakaoUser(String kakaoAccessToken) {
        KakaoUserResponse kakaoUser;
        try {
            kakaoUser = kakaoClient.getUserInfo(kakaoAccessToken);
        } catch (HttpClientErrorException e) {
            log.warn("[AuthService] 카카오 토큰 검증 실패 - status={}", e.getStatusCode());
            throw new CustomException(AuthErrorCode.INVALID_KAKAO_TOKEN);
        } catch (RestClientException e) {
            log.warn("[AuthService] 카카오 서버 통신 실패 - {}", e.getMessage());
            throw new CustomException(AuthErrorCode.KAKAO_SERVER_ERROR);
        }
        if (kakaoUser == null || kakaoUser.id() == null) {
            throw new CustomException(AuthErrorCode.INVALID_KAKAO_TOKEN);
        }
        return kakaoUser;
    }

    private String resolveNickname(KakaoUserResponse kakaoUser) {
        KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
        if (account != null && account.profile() != null && account.profile().nickname() != null) {
            return account.profile().nickname();
        }
        return DEFAULT_NICKNAME;
    }

    private String resolveEmail(KakaoUserResponse kakaoUser) {
        KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
        return account != null ? account.email() : null;
    }
}
