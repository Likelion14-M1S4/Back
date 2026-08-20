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

    // 카카오가 닉네임을 안 내려주는 경우(동의 안 함) 사용할 기본 닉네임 - nickname 컬럼은 NOT NULL이라 항상 값이 필요하다
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

    // 카카오 소셜 로그인(인가 코드 방식). 코드를 카카오 토큰으로 교환한 뒤 사용자 정보를 조회하고,
    // 최초 로그인이면 회원 생성 후 토큰 발급, 기존 회원이면 토큰만 재발급하고 refresh를 rotate한다
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

    // 토큰 재발급(rotate). refresh 토큰을 검증하고 DB에 저장된 값과 일치할 때만 새 access+refresh를 발급한다.
    // DB 값과 대조하므로 로그아웃(refresh null)했거나 더 최근 로그인/재발급으로 rotate된 옛 토큰은 거부된다(단일 세션).
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

    // 로그아웃. refresh 토큰을 무효화(null)해 이후 재발급을 막는다. access 토큰은 만료(60분)까지 유효하다(stateless).
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.UNAUTHORIZED));
        user.clearRefreshToken();
        log.info("[AuthService] 로그아웃 완료 - userId={}", userId);
    }

    // 회원 탈퇴(하드 삭제). 유저 소유 데이터는 삭제하고, 매장측 수령 기록(charm_receipt)은 계정 연결만 해제해 보존한다.
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

    // 인가 코드를 카카오 액세스 토큰으로 교환. 4xx(코드 무효/만료/redirect_uri 불일치)와 5xx(카카오 장애)를 각각 AuthErrorCode로 변환한다
    private String exchangeCodeForToken(String code, String redirectUri) {
        KakaoTokenResponse token;
        try {
            token = kakaoClient.getToken(code, redirectUri);
        } catch (HttpClientErrorException e) {
            // 카카오는 실패 사유를 KOE 코드로 내려주므로(예: KOE320 코드 재사용) 원인 추적용으로 body를 남긴다
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

    // 토큰으로 카카오 사용자 정보를 조회. 4xx(토큰 무효)와 5xx(카카오 장애)를 각각 AuthErrorCode로 변환한다
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

    // email은 선택값 - 카카오 미동의 시 null로 저장된다 (추후 사용자 확인/구매 연결의 키로 사용)
    private String resolveEmail(KakaoUserResponse kakaoUser) {
        KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
        return account != null ? account.email() : null;
    }
}
