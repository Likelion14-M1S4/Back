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

    // 개발 편의용 인증 우회 토글. AUTH_OPTIONAL_USER_ID가 설정되면 토큰 없는 요청을 해당 유저로 자동 인증한다.
    @Value("${auth.optional-user-id:}")
    private String optionalUserIdRaw;

    // SSE(SseEmitter) 응답은 ASYNC 디스패치로 필터 체인이 다시 타는데, 기본값(true)이면 이 필터가
    // 스킵돼 SecurityContext가 비어 AuthorizationDeniedException이 난다
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
            SecurityContextHolder.clearContext();
        }
        applyOptionalAuthFallback();
        filterChain.doFilter(request, response);
    }

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
