package com.itcotato.dortfolio.global.exception;

import com.itcotato.dortfolio.global.exception.types.GlobalErrorCode;
import com.itcotato.dortfolio.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException e
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.error(
                "CustomException: code={}, message={}",
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        log.error("MethodArgumentNotValidException", e);

        GlobalErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        String bindingMessage = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        bindingMessage
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        log.error("HttpRequestMethodNotSupportedException", e);

        GlobalErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException e
    ) {
        log.warn("AuthenticationException: {}", e.getMessage());

        GlobalErrorCode errorCode = GlobalErrorCode.UNAUTHORIZED;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(CsrfException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCsrfException(CsrfException e) {
        log.warn("CsrfException: {}", e.getMessage());

        GlobalErrorCode errorCode = GlobalErrorCode.INVALID_CSRF_TOKEN;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException e
    ) {
        log.warn("No resource found: {}", e.getResourcePath());

        GlobalErrorCode errorCode = GlobalErrorCode.NOT_FOUND;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Internal Server Exception", e);

        GlobalErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }
}
