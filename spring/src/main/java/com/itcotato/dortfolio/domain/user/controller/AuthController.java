package com.itcotato.dortfolio.domain.user.controller;

import com.itcotato.dortfolio.domain.user.controller.docs.AuthControllerDocs;
import com.itcotato.dortfolio.domain.user.dto.*;
import com.itcotato.dortfolio.domain.user.service.AuthService;
import com.itcotato.dortfolio.domain.user.service.PasswordResetService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /* 자체 회원가입 API */
    @Override
    @PostMapping("/signup")
    public ApiResponse<Void> signUp(
            @Valid
            @RequestBody SignUpRequest request
    ) {
        authService.signUp(request);
        return ApiResponse.success("회원가입이 성공적으로 완료되었습니다.");
    }

    /* 자체 로그인 API */
    @Override
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @Valid
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        return ApiResponse.success(
                "로그인이 성공적으로 완료되었습니다.",
                authService.login(request, httpRequest, response)
        );
    }

    /* Access Token 재발급 API */
    @Override
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        return ApiResponse.success(
                "Access Token이 성공적으로 재발급되었습니다.",
                authService.refresh(refreshToken, response)
        );
    }

    /* CSRF 토큰 발급 API */
    @Override
    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenResponse> issueCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ApiResponse.success(
                "CSRF 토큰이 성공적으로 발급되었습니다.",
                authService.issueCsrfToken(request, response)
        );
    }

    /* 로그아웃 API */
    @Override
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal UUID userId,
            HttpServletResponse response
    ) {
        authService.logout(userId, response);
        return ApiResponse.success("성공적으로 로그아웃되었습니다.");
    }

    /* 회원 탈퇴 API */
    @Override
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal UUID userId,
            HttpServletResponse response
    ) {
        authService.withdraw(userId, response);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }

    /* 비밀번호 재설정 요청 API */
    @Override
    @PostMapping("/reset-request")
    public ApiResponse<Void> requestPasswordReset(
            @Valid
            @RequestBody PasswordResetRequest request
            ) {
        passwordResetService.sendResetLink(request);
        return ApiResponse.success("비밀번호 재설정 메일이 성공적으로 발송되었습니다.");
    }

    /* 비밀번호 최종 변경 API */
    @Override
    @PatchMapping("/reset")
    public ApiResponse<Void> resetPassword(
            @Valid
            @RequestBody PasswordResetConfirmRequest request
            ) {
        passwordResetService.resetPassword(request);
        return ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.");
     }

}
