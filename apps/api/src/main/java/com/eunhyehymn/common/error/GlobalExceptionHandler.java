package com.eunhyehymn.common.error;

import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.infrastructure.security.JwtValidationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        var error = ErrorResponse.of(ex.getCode(), ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
        var error = ErrorResponse.of("validation_error", "Validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        var error = ErrorResponse.of("validation_error", "Validation failed", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        var error = ErrorResponse.of("unauthorized", "인증이 필요합니다", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        var error = ErrorResponse.of("forbidden", "접근 권한이 없습니다", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtValidation(JwtValidationException ex) {
        // Used by refresh-token flows and other JWT parsing; treat as client-side auth failure.
        var error = ErrorResponse.of("invalid_token", "토큰이 유효하지 않습니다", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler({
        NoHandlerFoundException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleNotFound(Exception ex) {
        var error = ErrorResponse.of("not_found", "요청한 리소스를 찾을 수 없습니다", null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        var error = ErrorResponse.of("method_not_allowed", "지원하지 않는 HTTP 메서드입니다", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        var error = ErrorResponse.of("unsupported_media_type", "지원하지 않는 Content-Type 입니다", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        var error = ErrorResponse.of("malformed_json", "요청 본문(JSON)을 파싱할 수 없습니다", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnhandled(Exception ex, HttpServletRequest request) {
        log.error(
            "Unhandled exception: method={} uri={} message={}",
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex
        );

        // Keep a stable envelope so clients can recover safely.
        var error = ErrorResponse.of("internal_error", "Unexpected server error", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }
}
