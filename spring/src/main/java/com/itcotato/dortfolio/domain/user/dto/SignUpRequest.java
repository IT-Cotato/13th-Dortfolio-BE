package com.itcotato.dortfolio.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

    // 이메일 검증
    @NotBlank(message = "이메일은 필수 입력 항복입니다.")
    @Size(max = 254, message = "이메일 전체 길이는 254자를 초과할 수 없습니다.")
    @Pattern(
            regexp = "^(?!\\.)(?!.*\\.\\.)[A-Za-z0-9._+\\-]{1,64}(?<!\\.)@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}$",
            message = "올바른 이메일 형식으로 입력해주세요. 예: cotato@gmail.com"
    )
    String email,

    // 비밀번호 검증
    @NotBlank(message = "비밀번호는 필수 입력항목입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,16}$",
            message = "비밀번호는 8~16자의 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    String password,

    // 이름 검증
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    String name
) {

}
