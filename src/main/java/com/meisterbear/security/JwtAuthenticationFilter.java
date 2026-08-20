package com.meisterbear.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // 개발 편의용 인증 우회 토글. AUTH_OPTIONAL_USER_ID가 설정되면 토큰 없는(또는 무효 토큰) 요청을
    // 해당 유저로 자동 인증한다. 프론트 연동 기간 한정으로 켜는 값 - 끌 때는 env 제거 후 재배포만 하면 된다.
    // 빈 문자열도 안전하게 "꺼짐"으로 처리하기 위해 String으로 받아 직접 파싱한다.
    @Value("${auth.optional-user-id:}")
    private String optionalUserIdRaw;

    // OncePerRequestFilter는 기본적으로 ASYNC 디스패치를 건너뛴다. SSE(SseEmitter) 응답은 완료 시점에
    // 컨테이너가 ASYNC 디스패치로 필터 체인을 다시 태우는데, 이 필터가 그때 스킵되면 SecurityContext가
    // 비어있는 채로 Spring Security의 AuthorizationFilter만 다시 돌아서 AuthorizationDeniedException이 난다
    // (응답이 이미 커밋된 뒤라 에러 페이지도 못 그리고 로그만 지저분하게 남음). false로 재정의해서 ASYNC 디스패치 때도
    // 이 필터가 같은 요청의 Authorization 헤더로 인증을 다시 채워주게 한다.
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        try {
            if (token != null && jwtProvider.validateToken(token)) {
                Long userId = jwtProvider.getUserId(token);
                CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 토큰이 유효하지 않거나 사용자 조회 실패 시 인증 정보 제거
            SecurityContextHolder.clearContext();
        }
        applyOptionalAuthFallback();
        filterChain.doFilter(request, response);
    }

    // 우회 토글이 켜져 있고 정상 인증이 없을 때만 지정 유저로 인증을 채운다.
    // 유저 조회 실패(없는 id 등) 시에는 조용히 넘어가 기존 401 흐름을 유지한다.
    private void applyOptionalAuthFallback() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        Long fallbackUserId = parseOptionalUserId();
        if (fallbackUserId == null) {
            return;
        }
        try {
            CustomUserDetails userDetails = customUserDetailsService.loadUserById(fallbackUserId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("[JwtAuthenticationFilter] 인증 우회 유저 조회 실패 - userId={}", fallbackUserId);
        }
    }

    private Long parseOptionalUserId() {
        if (optionalUserIdRaw == null || optionalUserIdRaw.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(optionalUserIdRaw.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
