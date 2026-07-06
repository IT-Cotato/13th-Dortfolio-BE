package com.itcotato.dortfolio.domain.user.service;

import com.itcotato.dortfolio.domain.user.dto.LoginRequest;
import com.itcotato.dortfolio.domain.user.dto.SignUpRequest;
import com.itcotato.dortfolio.domain.user.dto.TokenResponse;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.auth.JwtTokenProvider;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    /* 회원가입 비즈니스 로직 */
    @Transactional
    public void signUp(SignUpRequest request) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE) {
                @Override
                public String getMessage() {
                    return "이미 가입된 이메일 주소입니다. 다른 이메일을 입력해주세요.";
                }
            };
        }

        // 비밀번호 암호화 (BCrypt 해싱)
        String encodedPassword = passwordEncoder.encode(request.password());

        // 객체 생성
        User user = User.of(request.email(),
                encodedPassword,
                request.name()
        );

        // DB 저장
        userRepository.save(user);
    }

    /* 로그인 비즈니스 로직 */
    public TokenResponse login(LoginRequest request) {

        // 유저 정보 조회
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        // 비밀번호 비교
        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE) {
                @Override
                public String getMessage() {
                    return "비밀번호가 일치하지 않습니다.";
                }
            };
        }

        // 비밀번호가 일치하면 인증 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // Access, Refresh 토큰 발급 후 DTO 반환
        String accessToken = jwtTokenProvider.generateAccessToken(authenticationToken);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authenticationToken);

        return TokenResponse.of(accessToken, refreshToken);
    }
}
