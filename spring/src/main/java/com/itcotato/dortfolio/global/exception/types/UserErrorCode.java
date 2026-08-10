package com.itcotato.dortfolio.global.exception.types;

import com.itcotato.dortfolio.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 회원 정보입니다."),
    SOCIAL_USER_PASSWORD_RESET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U002", "소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다."),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "U003", "만료되었거나 유효하지 않은 비밀번호 재설정 토큰입니다."),
    RESET_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "U004", "잘못된 접근입니다. 이메일 정보가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U005", "이미 가입된 이메일 주소입니다. 다른 이메일을 입력해주세요."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.BAD_REQUEST, "U006", "이메일 또는 비밀번호가 일치하지 않습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "U007", "지원하지 않는 소셜 로그인 공급자입니다."),
    UNVERIFIED_SOCIAL_EMAIL(HttpStatus.BAD_REQUEST, "U008", "인증되지 않은 소셜 계정 이메일입니다. 이메일 인증 후 다시 시도해주세요."),
    SOCIAL_EMAIL_CONFLICT(HttpStatus.CONFLICT, "U009", "이미 다른 방식으로 가입된 이메일입니다. 기존 계정으로 로그인을 이용해주세요."),

    INVALID_AUTHORITY_TOKEN(HttpStatus.UNAUTHORIZED, "U010", "권한 정보가 없는 유효하지 않은 토큰입니다."),
    INVALID_TOKEN_SIGNATURE(HttpStatus.UNAUTHORIZED, "U011", "잘못된 JWT 서명입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "U012", "만료된 JWT 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "U013", "지원되지 않는 JWT 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.BAD_REQUEST, "U014", "JWT 토큰이 비어있거나 잘못되었습니다."),
    INVALID_PROFILE_IMAGE_KEY(HttpStatus.BAD_REQUEST, "U015", "유효하지 않은 프로필 이미지 경로입니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
