package com.itcotato.dortfolio.domain.user.controller;

import com.itcotato.dortfolio.domain.user.dto.SignUpRequest;
import com.itcotato.dortfolio.domain.user.service.AuthService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증/인가 (Auth)", description = "회원가입, 로그인, 토큰 재발급 등 인증 관련 API입니다.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

}
