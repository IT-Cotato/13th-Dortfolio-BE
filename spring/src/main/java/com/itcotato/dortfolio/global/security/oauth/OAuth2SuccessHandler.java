package com.itcotato.dortfolio.global.security.oauth;

import com.itcotato.dortfolio.global.security.jwt.JwtTokenProvider;
import com.itcotato.dortfolio.global.util.CookieUtil;
import com.itcotato.dortfolio.global.util.RedisUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CsrfTokenRepository csrfTokenRepository;
    private final CookieUtil cookieUtil;
    private final RedisUtil redisUtil;

    @Value("${app.oauth2.success-redirect-uri}")
    private String successRedirectUri;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMillis;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        UUID userId = oAuth2User.getUserId();

        String accessToken = jwtTokenProvider.generateAccessToken(authentication, userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, userId);

        log.info("OAuth2 로그인 성공. 자체 JWT 토큰을 발급합니다.");

        redisUtil.setDataExpire("RT:" + userId, refreshToken, refreshExpirationMillis);
        cookieUtil.addAccessTokenCookie(response, accessToken);
        cookieUtil.addRefreshTokenCookie(response, refreshToken, true);
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        cookieUtil.addCsrfTokenCookie(response, csrfToken.getToken(), true);

        getRedirectStrategy().sendRedirect(request, response, successRedirectUri);
    }
}
