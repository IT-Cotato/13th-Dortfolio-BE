package com.itcotato.dortfolio.domain.user.service;

import com.itcotato.dortfolio.global.security.oauth.GoogleUserInfo;
import com.itcotato.dortfolio.global.security.oauth.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 기본 DefaultOAuth2UsrService를 통해 구글로부터 유저 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 어떤 소셜 로그인 공급자인지 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 진행 중인 Provider: {}", registrationId);

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
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인 공급자입니다.");
        }

        log.info("구글 로그인 시도가 이메일: {}", oAuth2UserInfo.getEmail());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2UserInfo.getAttributes(),
                userNameAttributeName
        );
    }
}
