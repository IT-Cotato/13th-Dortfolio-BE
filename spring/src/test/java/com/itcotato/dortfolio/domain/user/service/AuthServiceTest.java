package com.itcotato.dortfolio.domain.user.service;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.activity.service.ActivityTypeService;
import com.itcotato.dortfolio.domain.user.dto.TokenResponse;
import com.itcotato.dortfolio.domain.user.dto.SignUpRequest;
import com.itcotato.dortfolio.domain.user.entity.Role;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.domain.user.repository.UserTermAgreementRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import com.itcotato.dortfolio.global.security.jwt.JwtTokenProvider;
import com.itcotato.dortfolio.global.security.user.CustomUserDetailsService;
import com.itcotato.dortfolio.global.util.CookieUtil;
import com.itcotato.dortfolio.global.util.RedisUtil;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserTermAgreementRepository userTermAgreementRepository;
    @Mock private UserJobRepository userJobRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisUtil redisUtil;
    @Mock private CookieUtil cookieUtil;
    @Mock private ActivityTypeService activityTypeService;
    @Mock private CsrfTokenRepository csrfTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void reissuesAccessTokenWithValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = org.mockito.Mockito.mock(User.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.getRefreshTokenUserId("refresh-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(Role.USER);
        when(redisUtil.getData("RT:" + userId)).thenReturn("refresh-token");
        when(jwtTokenProvider.generateAccessToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(userId)))
                .thenReturn("new-access-token");

        TokenResponse tokenResponse = authService.refresh("refresh-token", response);

        assertThat(tokenResponse.grantType()).isEqualTo("Bearer");
        assertThat(tokenResponse.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    void rejectsRefreshTokenThatDoesNotMatchRedis() {
        UUID userId = UUID.randomUUID();
        User user = org.mockito.Mockito.mock(User.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.getRefreshTokenUserId("request-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisUtil.getData("RT:" + userId)).thenReturn("saved-token");

        assertThatThrownBy(() -> authService.refresh("request-token", response))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);

        verify(cookieUtil).deleteCookie(response, "refreshToken");
        verify(cookieUtil).deleteCookie(response, "XSRF-TOKEN");
    }

    @Test
    void rejectsRefreshAfterLogoutRemovesRedisToken() {
        UUID userId = UUID.randomUUID();
        User user = org.mockito.Mockito.mock(User.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.getRefreshTokenUserId("refresh-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisUtil.getData("RT:" + userId)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh("refresh-token", response))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void rejectsMissingRefreshToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.refresh(null, response))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void clearsCookiesWhenRefreshTokenParsingFails() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.getRefreshTokenUserId("invalid-token"))
                .thenThrow(new CustomException(UserErrorCode.INVALID_REFRESH_TOKEN));

        assertThatThrownBy(() -> authService.refresh("invalid-token", response))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.INVALID_REFRESH_TOKEN);

        verify(cookieUtil).deleteCookie(response, "refreshToken");
        verify(cookieUtil).deleteCookie(response, "XSRF-TOKEN");
    }

    @Test
    void rejectsSignUpWhenEmailBelongsToGoogleAccount() {
        SignUpRequest request = createSignUpRequest("google@example.com");

        User googleUser = User.createSocialUser(
                "google@example.com",
                "구글사용자",
                "GOOGLE",
                "google-provider-id"
        );

        when(userRepository.findByEmail("google@example.com"))
                .thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception ->
                        ((CustomException) exception).getErrorCode()
                )
                .isEqualTo(UserErrorCode.GOOGLE_ACCOUNT_ALREADY_EXISTS);

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsSignUpWhenEmailBelongsToLocalAccount() {
        SignUpRequest request = createSignUpRequest("local@example.com");

        User localUser = User.of(
                "local@example.com",
                "encoded-password",
                "일반사용자"
        );

        when(userRepository.findByEmail("local@example.com"))
                .thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception ->
                        ((CustomException) exception).getErrorCode()
                )
                .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);

        verifyNoInteractions(passwordEncoder);
    }

    private SignUpRequest createSignUpRequest(String email) {
        return new SignUpRequest(
                email,
                "Password1!",
                "테스트사용자",
                true,
                false
        );
    }
}
