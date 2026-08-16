package com.itcotato.dortfolio.global.security.jwt;

import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessExpirationTime;
    private final long refreshExpirationTime;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessExpirationTime,
            @Value("${jwt.refresh-expiration}") long refreshExpirationTime
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationTime = accessExpirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
    }

    /* Access Token 생성 */
    public String generateAccessToken(Authentication authentication, UUID userId) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + accessExpirationTime);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("userId", userId.toString())
                .claim("auth", authorities)
                .claim("tokenType", "ACCESS")
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* Refresh Token 생성 */
    public String generateRefreshToken(Authentication authentication, UUID userId) {
        long now = (new Date()).getTime();
        Date refreshTokenExpiresIn = new Date(now + refreshExpirationTime);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("userId", userId.toString())
                .claim("tokenType", "REFRESH")
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* JWT 토큰을 복호화하여 Spring Security의 Authentication 객체 생성 */
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new CustomException(UserErrorCode.INVALID_AUTHORITY_TOKEN);
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new CustomException(UserErrorCode.INVALID_AUTHORITY_TOKEN);
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new CustomException(UserErrorCode.INVALID_AUTHORITY_TOKEN);
        }

        return new UsernamePasswordAuthenticationToken(userId, "", authorities);
    }

    /* 토큰 유효성 및 만료 기간 검증 */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("JWT 검증 실패: {}", UserErrorCode.INVALID_TOKEN_SIGNATURE.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("JWT 검증 실패: {}", UserErrorCode.EXPIRED_TOKEN.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("JWT 검증 실패: {}", UserErrorCode.UNSUPPORTED_TOKEN.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("JWT 검증 실패: {}", UserErrorCode.EMPTY_TOKEN.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
