package com.itcotato.dortfolio.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

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

    @Builder(access = AccessLevel.PRIVATE)
    private User(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public static User of(String email, String encodedPassword, String name) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.USER)
                .build();
    }

}
