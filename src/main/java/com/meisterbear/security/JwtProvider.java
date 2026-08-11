package com.meisterbear.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String ACCESS_TOKEN_TYPE = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH_TOKEN";

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(CustomUserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return buildToken(userDetails.getUser().getId(), ACCESS_TOKEN_TYPE, roles, accessTokenExpiration);
    }

    public String createRefreshToken(CustomUserDetails userDetails) {
        return buildToken(userDetails.getUser().getId(), REFRESH_TOKEN_TYPE, null, refreshTokenExpiration);
    }

    private String buildToken(Long userId, String type, List<String> roles, long expiration) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + expiration);
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiredAt)
                .signWith(secretKey);
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.compact();
    }

    // 서명·만료 검증 + Access Token 타입 확인
    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return ACCESS_TOKEN_TYPE.equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(extractClaims(token).getSubject());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
