package com.itcotato.dortfolio.global.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.web.util.matcher.RequestMatcher;

final class CookieAuthenticatedCsrfProtectionMatcher implements RequestMatcher {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");
    private static final String TOKEN_REFRESH_PATH = "/api/auth/refresh";

    @Override
    public boolean matches(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }

        if (TOKEN_REFRESH_PATH.equals(request.getRequestURI())) {
            return true;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }
}
