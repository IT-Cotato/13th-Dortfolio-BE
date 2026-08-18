package com.itcotato.dortfolio.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesErrorCodeInFailureResponse() throws Exception {
        ApiResponse<Void> response = ApiResponse.fail(
                "U018",
                "이미 구글로 가입된 이메일입니다. 구글 로그인을 이용해주세요."
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"code\":\"U018\"");
        assertThat(json).contains(
                "\"message\":\"이미 구글로 가입된 이메일입니다. 구글 로그인을 이용해주세요.\""
        );
        assertThat(json).doesNotContain("\"data\"");
    }

    @Test
    void omitsCodeFromSuccessResponse() throws Exception {
        ApiResponse<Void> response =
                ApiResponse.success("회원가입이 성공적으로 완료되었습니다.");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"success\":true");
        assertThat(json).contains(
                "\"message\":\"회원가입이 성공적으로 완료되었습니다.\""
        );
        assertThat(json).doesNotContain("\"code\"");
        assertThat(json).doesNotContain("\"data\"");
    }
}