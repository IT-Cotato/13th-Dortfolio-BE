package com.itcotato.dortfolio.global.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    private final Duration accessTokenMaxAge;
    private final Duration refreshTokenMaxAge;

    public CookieUtil(
            @Value("${jwt.access-expiration}") long accessExpirationMillis,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMillis
    ) {
        this.accessTokenMaxAge = Duration.ofMillis(accessExpirationMillis);
        this.refreshTokenMaxAge = Duration.ofMillis(refreshExpirationMillis);
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addHttpOnlyCookie(response, "accessToken", accessToken, accessTokenMaxAge);
    }

    public void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken,
            boolean rememberMe
    ) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = httpOnlyCookieBuilder("refreshToken", refreshToken);

        if (rememberMe) {
            cookieBuilder.maxAge(refreshTokenMaxAge);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    private void addHttpOnlyCookie(
            HttpServletResponse response,
            String name,
            String value,
            Duration maxAge
    ) {
        ResponseCookie cookie = httpOnlyCookieBuilder(name, value)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder httpOnlyCookieBuilder(String name, String value) {
        return ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None");
    }

    /* 지정한 이름의 쿠키를 만료(삭제) 메서드 */
    public void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(!"XSRF-TOKEN".equals(name))
                .secure(true)
                .sameSite("None")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
