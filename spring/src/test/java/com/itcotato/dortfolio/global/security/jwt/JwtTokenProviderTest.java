package com.itcotato.dortfolio.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private Authentication authentication;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                1_800_000,
                1_209_600_000
        );
        authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        userId = UUID.randomUUID();
    }

    @Test
    void validatesOnlyAccessTokenAsAccessToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, userId);

        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isFalse();
    }

    @Test
    void extractsUserIdOnlyFromValidRefreshToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, userId);

        assertThat(jwtTokenProvider.getRefreshTokenUserId(refreshToken)).isEqualTo(userId);
        assertInvalidRefreshToken(accessToken);
    }

    @Test
    void rejectsMalformedRefreshToken() {
        assertInvalidRefreshToken("invalid-token");
    }

    private void assertInvalidRefreshToken(String token) {
        assertThatThrownBy(() -> jwtTokenProvider.getRefreshTokenUserId(token))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }
}
