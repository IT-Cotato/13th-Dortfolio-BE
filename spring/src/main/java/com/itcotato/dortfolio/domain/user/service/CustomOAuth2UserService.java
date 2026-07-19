package com.itcotato.dortfolio.domain.user.service;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.security.oauth.GoogleUserInfo;
import com.itcotato.dortfolio.global.security.oauth.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 기본 DefaultOAuth2UsrService를 통해 구글로부터 유저 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 어떤 소셜 로그인 공급자인지 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 구글에서 제공하는 고유의 속성 키값 추출
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // 추출한 raw 데이터 구조(Map) 분리
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 규격에 맞게 파싱 처리
        OAuth2UserInfo oAuth2UserInfo = null;
        if ("google".equalsIgnoreCase(registrationId)) {
            oAuth2UserInfo = new GoogleUserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException(new OAuth2Error("INVALID_PROVIDER"), "지원하지 않는 소셜 로그인 공급자입니다.");
        }

        // 구글 계정의 이메일 인증 여부 검증
        Boolean isEmailVerified = (Boolean) attributes.get("email_verified");
        if (isEmailVerified != null && !isEmailVerified) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("UNVERIFIED_EMAIL"),
                    "인증되지 않은 소셜 계정 이메일입니다. 이메일 인증 후 다시 시도해주세요."
            );
        }

        String email = oAuth2UserInfo.getEmail();
        String provider = registrationId.toUpperCase();
        String providerId = oAuth2UserInfo.getProviderId();

        // 기존 소셜 유저 우선 조회
        Optional<User> socialUserOptional = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (socialUserOptional.isPresent()) {
            user = socialUserOptional.get();
        } else {
            // 소셜 계정이 없다면, 혹시 동일한 이메일로 가입된 기존 계정이 있는지 확인
            Optional<User> emailUserOptional = userRepository.findByEmail(email);

            if (emailUserOptional.isPresent()) {
                User existingUser = emailUserOptional.get();

                log.warn("소셜 로그인 충돌 발생 = 기존 가입 방식: {}", existingUser.getProvider());
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("EMAIL_CONFLICT"),
                        "이미 다른 방식으로 등록된 이메일입니다. 기존 계정으로 로그인을 이용해주세요."
                );
            }

            // 중복된 이메일도 없다면 신규 소셜 유저로 자동 회원가입 진행
            user = User.createSocialUser(
                    email,
                    oAuth2UserInfo.getName(),
                    provider,
                    providerId
            );
            userRepository.save(user);
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2UserInfo.getAttributes(),
                "email"
        );
    }
}
