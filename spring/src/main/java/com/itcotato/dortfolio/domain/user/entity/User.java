package com.itcotato.dortfolio.domain.user.entity;

import com.itcotato.dortfolio.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    // 이메일
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 비밀번호
    @Column(length = 255)
    private String password;

    // 닉네임
    @Column(nullable = false, length = 50)
    private String nickname;

    // 권한 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 소셜 로그인 제공자 (LOCAL, GOOGLE 등)
    @Column(length = 50)
    private String provider;

    // 소셜 로그인 고유 ID
    @Column(name = "provider_id", length = 255)
    private String providerId;

    private User(String email, String password, String nickname, Role role,
                 String provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 일반 회원가입용 메서드
    public static User of(String email, String encodedPassword, String nickname) {
        return new User(
                email,
                encodedPassword,
                nickname,
                Role.USER,
                "LOCAL",
                null
        );
    }

    // 소셜 가입용 메서드
    public static User createSocialUser(String email, String nickname, String provider, String providerId) {
        return new User(
                email,
                null,
                nickname,
                Role.USER,
                provider.toUpperCase(),
                providerId
        );
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public boolean isLocalUser() {
        return "LOCAL".equalsIgnoreCase(this.provider);
    }

}
