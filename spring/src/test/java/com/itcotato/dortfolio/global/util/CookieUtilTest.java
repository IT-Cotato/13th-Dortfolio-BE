package com.itcotato.dortfolio.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieUtilTest {

    private CookieUtil cookieUtil;

    @BeforeEach
    void setUp() {
        cookieUtil = new CookieUtil(1_800_000, 1_209_600_000);
    }

    @Test
    void createsPersistentRefreshAndCsrfCookiesWhenRememberMeIsEnabled() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addRefreshTokenCookie(response, "refresh-token", true);
        cookieUtil.addCsrfTokenCookie(response, "csrf-token", true);

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).allMatch(cookie -> cookie.contains("Max-Age=1209600"));
        assertThat(cookies.get(0)).contains("HttpOnly", "Secure", "SameSite=None");
        assertThat(cookies.get(1)).doesNotContain("HttpOnly").contains("Secure", "SameSite=None");
    }

    @Test
    void createsSessionRefreshAndCsrfCookiesWhenRememberMeIsDisabled() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addRefreshTokenCookie(response, "refresh-token", false);
        cookieUtil.addCsrfTokenCookie(response, "csrf-token", false);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .noneMatch(cookie -> cookie.contains("Max-Age"));
    }
}
