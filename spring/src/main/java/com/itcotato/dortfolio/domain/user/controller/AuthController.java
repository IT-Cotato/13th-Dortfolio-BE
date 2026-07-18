package com.itcotato.dortfolio.domain.user.controller;

import com.itcotato.dortfolio.domain.user.dto.*;
import com.itcotato.dortfolio.domain.user.service.AuthService;
import com.itcotato.dortfolio.domain.user.service.PasswordResetService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Tag(name = "인증/인가 (Auth)", description = "회원가입, 로그인, 토큰 재발급 등 인증 관련 API입니다.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /* 자체 회원가입 API */
    @PostMapping("/signup")
    public ApiResponse<Void> signUp(
            @Valid
            @RequestBody SignUpRequest request
    ) {
        authService.signUp(request);
        return ApiResponse.success("회원가입이 성공적으로 완료되었습니다.");
    }

    /* 자체 로그인 API */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @Valid
            @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(
                "로그인이 성공적으로 완료되었습니다.",
                authService.login(request)
        );
    }

    /* 비밀번호 재설정 요청 API */
    @PostMapping("/reset-request")
    public ApiResponse<Void> requestPasswordReset(
            @Valid
            @RequestBody PasswordResetRequest request
            ) {
        passwordResetService.sendResetLink(request);
        return ApiResponse.success("비밀번호 재설정 메일이 성공적으로 발송되었습니다.");
    }

    /* 비밀번호 최종 변경 API */
    @PatchMapping("/reset")
    public ApiResponse<Void> resetPassword(
            @Valid
            @RequestBody PasswordResetResponse request
            ) {
        passwordResetService.resetPassword(request);
        return ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.");
     }
}
