package com.quickbite.quickbite.common.exception;

import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.exception.MaxSessionException;
import com.quickbite.quickbite.onboarding.exception.ApplicationNotFoundException;
import com.quickbite.quickbite.onboarding.exception.ApplicationStateException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import com.quickbite.quickbite.common.exception.BadRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Validation Exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        problemDetail.setDetail("One or more validation errors occurred.");

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                validationErrors.putIfAbsent(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                )
        );
        problemDetail.setProperty("validationErrors", validationErrors);

        return problemDetail;
    }

    // 404 status exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Requested resource was not found");
        return problemDetail;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Authentication failed");
        return problemDetail;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequestException(BadRequestException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Bad request");
        return problemDetail;
    }

    @ExceptionHandler(MaxSessionException.class)
    public ResponseEntity<com.quickbite.quickbite.auth.dto.SessionLimitErrorResponse> handleMaxSessionException(MaxSessionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new com.quickbite.quickbite.auth.dto.SessionLimitErrorResponse(
                        "session_limit_exceeded",
                        ex.getSessionManagementToken(),
                        ex.getMaxSessions()));
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ProblemDetail handleApplicationNotFoundException(ApplicationNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Application Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(ApplicationStateException.class)
    public ProblemDetail handleApplicationStateException(ApplicationStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Invalid Application State");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    // Unknown exception
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail("An unexpected error occurred.");
        problemDetail.setProperty("exceptionType", e.getClass().getSimpleName());
        return problemDetail;
    }
}
