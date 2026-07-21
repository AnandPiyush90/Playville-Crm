package com.playville.crm.common;

import com.playville.crm.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req, null);
    }

    @ExceptionHandler(BranchAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleBranchDenied(
            BranchAccessDeniedException ex, HttpServletRequest req) {
        log.warn("Branch access violation at {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied at {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), req, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {
        log.warn("Authentication failed at {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Invalid username or password", req, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), req, null);
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<ErrorResponse> handleApiConflict(
            ApiConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation at {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Conflict",
                "A duplicate or conflicting record already exists", req, null);
    }

    @ExceptionHandler(CustomerPhoneExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerPhoneExists(CustomerPhoneExistsException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CUSTOMER_PHONE_EXISTS", ex.getMessage(), req, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, errorCode(ex.getMessage(), "Business Rule Violation"),
                ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a   // keep first on duplicate field
                        , LinkedHashMap::new
                ));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage() != null ? violation.getMessage() : "Invalid value",
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = Map.of(ex.getParameterName(), "This parameter is required");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String name = ex.getName() != null ? ex.getName() : "request";
        Map<String, String> fieldErrors = Map.of(name, "Invalid value");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "Invalid request body", req, Map.of("request", "Check field values and formats"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: ", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", req, null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message,
            HttpServletRequest req, Map<String, String> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .code(errorCode(message, error))
                .message(message)
                .path(req.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private String errorCode(String message, String fallback) {
        if (message != null && message.matches("^[A-Z][A-Z0-9_]+:.*")) {
            return message.substring(0, message.indexOf(':'));
        }
        return fallback;
    }
}
