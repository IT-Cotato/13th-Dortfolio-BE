package com.itcotato.dortfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CookieAuthenticatedCsrfProtectionMatcherTest {

    private final CookieAuthenticatedCsrfProtectionMatcher matcher =
            new CookieAuthenticatedCsrfProtectionMatcher();

    @Test
    void protectsStateChangingRequestAuthenticatedByCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/members/me");
        request.setCookies(new Cookie("accessToken", "access-token"));

        assertThat(matcher.matches(request)).isTrue();
    }

    @Test
    void doesNotRequireCsrfForSafeCookieAuthenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.setCookies(new Cookie("accessToken", "access-token"));

        assertThat(matcher.matches(request)).isFalse();
    }

    @Test
    void doesNotRequireCsrfForBearerOnlyRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/members/me");
        request.addHeader("Authorization", "Bearer access-token");

        assertThat(matcher.matches(request)).isFalse();
    }
}
