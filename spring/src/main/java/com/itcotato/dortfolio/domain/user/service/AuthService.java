package com.itcotato.dortfolio.domain.user.service;

import com.itcotato.dortfolio.domain.user.dto.LoginRequest;
import com.itcotato.dortfolio.domain.user.dto.SignUpRequest;
import com.itcotato.dortfolio.domain.user.dto.TokenResponse;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.entity.UserTermAgreement;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.domain.user.repository.UserTermAgreementRepository;
import com.itcotato.dortfolio.global.auth.JwtTokenProvider;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import com.itcotato.dortfolio.global.exception.types.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    /* 회원가입 비즈니스 로직 */
    @Transactional
    public void signUp(SignUpRequest request) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 비밀번호 암호화 (BCrypt 해싱)
        String encodedPassword = passwordEncoder.encode(request.password());

        // 유저 객체 생성
        User user = User.of(
                request.email(),
                encodedPassword,
                request.nickname()
        );

        // DB 저장 및 동시성 중복 가입 예외 처리
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 개별 동의 내역 저장
        userTermAgreementRepository.save(
                UserTermAgreement.create(user, "TERMS_OF_SERVICE", request.isTermsAgreed())
        );
        userTermAgreementRepository.save(
                UserTermAgreement.create(user, "PRIVACY_POLICY", request.isPrivacyAgreed())
        );
        userTermAgreementRepository.save(
                UserTermAgreement.create(user, "MARKETING", request.isMarketingAgreed())
        );
    }

    /* 로그인 비즈니스 로직 */
    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(UserErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (!user.isLocalUser()) {
            throw new CustomException(UserErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new CustomException(UserErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String accessToken = jwtTokenProvider.generateAccessToken(authenticationToken);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authenticationToken);

        return TokenResponse.of(accessToken, refreshToken);
    }
}
