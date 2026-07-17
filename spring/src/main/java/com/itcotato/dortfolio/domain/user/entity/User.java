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
        return new User(
                email,
                encodedPassword,
                name,
                Role.USER,
                isTermsAgreed,
                isPrivacyAgreed,
                isMarketingAgreed
        );
    }

}
