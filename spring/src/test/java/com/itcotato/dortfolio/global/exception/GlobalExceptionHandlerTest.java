package com.itcotato.dortfolio.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.global.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesGlobalValidationErrorWithoutFieldError() {
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new ObjectError("signUpRequest", "약관 동의가 필요합니다.")
        ));

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMethodArgumentNotValidException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("G001");
        assertThat(response.getBody().getMessage())
                .isEqualTo("약관 동의가 필요합니다.");
    }

    @Test
    void handlesMissingCsrfTokenAsForbidden() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleCsrfException(new MissingCsrfTokenException(null));

        assertCsrfForbiddenResponse(response);
    }

    @Test
    void handlesInvalidCsrfTokenAsForbidden() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleCsrfException(new InvalidCsrfTokenException(
                        new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "expected-token"),
                        "invalid-token"
                ));

        assertCsrfForbiddenResponse(response);
    }

    private void assertCsrfForbiddenResponse(ResponseEntity<ApiResponse<Void>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("G006");
        assertThat(response.getBody().getMessage()).isEqualTo("CSRF 토큰이 유효하지 않습니다.");
    }
}
