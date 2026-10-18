package com.ankit.orderservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("errors", List.of(message)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @ExceptionHandler(OrderExceptions.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(OrderExceptions.NotFound ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OrderExceptions.InvalidTransition.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(OrderExceptions.InvalidTransition ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OrderExceptions.Unauthorized.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(OrderExceptions.Unauthorized ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(OrderExceptions.CsrfRejected.class)
    public ResponseEntity<Map<String, Object>> handleCsrf(OrderExceptions.CsrfRejected ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(OrderExceptions.RateLimited.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(OrderExceptions.RateLimited ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(OrderExceptions.InvalidBulkRequest.class)
    public ResponseEntity<Map<String, Object>> handleBulkRequest(OrderExceptions.InvalidBulkRequest ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(OrderExceptions.AdminDisabled.class)
    public ResponseEntity<Map<String, Object>> handleAdminDisabled(OrderExceptions.AdminDisabled ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}