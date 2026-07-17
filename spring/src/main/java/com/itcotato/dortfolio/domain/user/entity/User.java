package com.itcotato.dortfolio.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    // PK
    @Id
    @Column(name = "user_id", updatable = false, nullable = false, length = 36)
    private String id;

    // 이메일
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 비밀번호
    @Column(nullable = false)
    private String password;

    // 이름
    @Column(nullable = false, length = 50)
    private String name;

    // 권한 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 이용 약관
    @Column(nullable = false)
    private boolean isTermsAgreed;

    // 개인정보 수집 및 이용 동의
    @Column(nullable = false)
    private boolean isPrivacyAgreed;

    // 선택 사항
    @Column(nullable = false)
    private boolean isMarketingAgreed;

    @PrePersist
    public void createUuid() {
        this.id = UUID.randomUUID().toString();
    }

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, String name, Role role,
                 boolean isTermsAgreed, boolean isPrivacyAgreed, boolean isMarketingAgreed) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.isTermsAgreed = isTermsAgreed;
        this.isPrivacyAgreed = isPrivacyAgreed;
        this.isMarketingAgreed = isMarketingAgreed;
    }

    public static User of(String email, String encodedPassword, String name,
                          boolean isTermsAgreed, boolean isPrivacyAgreed, boolean isMarketingAgreed) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.USER)
                .isTermsAgreed(isTermsAgreed)
                .isPrivacyAgreed(isPrivacyAgreed)
                .isMarketingAgreed(isMarketingAgreed)
                .build();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

}
