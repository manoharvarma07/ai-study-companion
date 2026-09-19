package com.aistudy.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return err(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> forbidden(RuntimeException ex, HttpServletRequest req) {
        return err(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthorized(AuthenticationException ex, HttpServletRequest req) {
        return err(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required", req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest req) {
        return err(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + (f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage()))
                .findFirst().orElse("Validation failed");
        return err(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(Exception ex, HttpServletRequest req) {
        return err(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return err(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Invalid value for '" + ex.getName() + "'", req);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> malformed(Exception ex, HttpServletRequest req) {
        return err(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request", req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return err(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "File too large (max 100MB)", req);
    }

    @ExceptionHandler(AiException.class)
    public ResponseEntity<ApiError> aiFailure(AiException ex, HttpServletRequest req) {
        log.warn("AI failure on {}: {}", req.getRequestURI(), ex.getMessage());
        return err(HttpStatus.BAD_GATEWAY, "AI_ERROR", "AI service temporarily unavailable. Please try again.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error on {}", req.getRequestURI(), ex);
        return err(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
    }

    private ResponseEntity<ApiError> err(HttpStatus s, String code, String msg, HttpServletRequest req) {
        return ResponseEntity.status(s).body(new ApiError(s, code, msg, req.getRequestURI()));
    }
}
