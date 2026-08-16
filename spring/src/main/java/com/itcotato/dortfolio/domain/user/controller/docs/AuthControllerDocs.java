package com.itcotato.dortfolio.domain.user.controller.docs;

import com.itcotato.dortfolio.domain.user.dto.*;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Auth", description = "인증/인가 API")
public interface AuthControllerDocs {

    @Operation(summary = "자체 회원가입 API", description = "이메일, 비밀번호, 닉네임 및 약관 동의 정보를 받아 자체 회원가입을 진행합니다.")
    ApiResponse<Void> signUp(
            @Valid @RequestBody SignUpRequest request
    );

    @Operation(
            summary = "자체 로그인 API",
            description = "이메일과 비밀번호로 로그인합니다. Access Token은 응답 본문으로 반환하고, "
                    + "Refresh Token은 HttpOnly 쿠키로 발급합니다. rememberMe는 브라우저 종료 후 로그인 유지 여부를 나타냅니다."
    )
    ApiResponse<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    );

    @Operation(summary = "로그아웃 API", description = "Redis에 저장된 Refresh Token을 삭제하고, 클라이언트의 Token 쿠키를 만료(삭제)시킵니다.")
    ApiResponse<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            HttpServletResponse response
    );

    @Operation(summary = "회원 탈퇴 API", description = "회원 탈퇴 시 DB의 연관 데이터와 회원을 영구 삭제(Hard Delete)하고, Redis 토큰 및 쿠키를 만료시켜 로그아웃 처리합니다.")
    ApiResponse<Void> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            HttpServletResponse response
    );

    @Operation(summary = "비밀번호 재설정 메일 요청 API", description = "가입된 이메일로 비밀번호 재설정 링크를 발송합니다.")
    ApiResponse<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    );

    @Operation(summary = "비밀번호 최종 변경 API", description = "메일로 발송된 토큰을 검증한 후 새 비밀번호로 변경합니다.")
    ApiResponse<Void> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request
    );
}
