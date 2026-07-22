package com.itcotato.dortfolio.global.security.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final UUID userId;
    private final String email;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            UUID userId,
                            String email) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
        this.email = email;
    }
}