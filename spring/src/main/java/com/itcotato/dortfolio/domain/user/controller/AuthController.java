package com.itcotato.dortfolio.domain.user.controller;

import com.itcotato.dortfolio.domain.user.dto.LoginRequest;
import com.itcotato.dortfolio.domain.user.dto.PasswordResetResponse;
import com.itcotato.dortfolio.domain.user.dto.SignUpRequest;
import com.itcotato.dortfolio.domain.user.dto.TokenResponse;
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
    @Operation(
            summary = "자체 회원가입",
            description = "이메일 계정, 비밀번호, 이름을 입력받아 새로운 유저를 등록합니다." +
                    "엄격한 이메일/비밀번호 정규식 검증이 포함되어 있습니다."
    )

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "G001: 올바르지 않은 입력값 (이메일/비밀번호 정규식 위반, 필수값 누락 등)\n\n" +
                            "U001: 이미 가입된 이메일 주소 사용 시 발생"
            )
    })

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(
            @Valid @RequestBody SignUpRequest request
            ) {
        authService.signUp(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 성공적으로 완료되었습니다."));
    }

    /* 자체 로그인 API */
    @Operation(
            summary = "자체 로그인",
            description = "이메일 계정과 비밀번호를 받아 유저를 검증하고, 인증에 필요한 JWT Access 및 Refresh 토큰을 발급합니다."
    )

    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 (Access/Refresh 토큰 묶음 반환)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "G001: 이메일 형식이 누락되었거나 바르지 않은 경우\n\n" +
                            "INVALID_INPUT_VALUE: 비밀번호가 일치하지 않거나 가입되지 않은 이메일인 경우"
            )
    })

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokenResponse = authService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그인이 성공적으로 완료되었습니다.", tokenResponse));
    }

    /* 비밀번호 재설정 요청 API */
    @Operation(
            summary = "비밀번호 재설정 메일 요청",
            description = "입력한 이메일 계정의 존재 여부 및 로컬 회원 여부를 확인한 후, 비밀번호 변경을 위한 일회성 보안 링크 메일을 발송합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 메일 발송 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "U002: 소셜 로그인 계정은 비밀번호를 재설정할 수 없음\n\n" +
                            "G001: 이메일 형식이 잘못되었거나 누락된 경우"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "U001: 존재하지 않는 회원 이메일인 경우"
            )
    })
    @PostMapping("reset-request")
    public ResponseEntity<ApiResponse<Void>> reqyestPasswordReset(
            @Valid @RequestBody PaswordResetRequest request
    ) {
        passwordResetService.sendResetLink(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("비밀번호 재설정 메일이 성공적으로 발송되었습니다."));
    }

    /* 비밀번호 최종 변경 API ✨ */
    @Operation(
            summary = "비밀번호 최종 재설정",
            description = "메일 링크를 통해 넘어온 일회성 UUID 토큰과 이메일을 교차 검증한 후, 새 비밀번호로 암호화하여 변경 처리를 완료합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "U003: 만료되었거나 유효하지 않은 비밀번호 재설정 토큰\n\n" +
                            "U004: 입력된 이메일과 토큰 매핑 정보가 일치하지 않는 교차 검증 오류\n\n" +
                            "G001: 비밀번호 복잡도(8~16자 영문, 숫자, 특수문자 조합) 정책 위반"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "U001: 이메일에 해당하는 유저를 조회할 수 없음"
            )
    })
    @PatchMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetResponse request
    ) {
        passwordResetService.resetPassword(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("비밀번호가 성공적으로 변경되었습니다."));
    }

}
