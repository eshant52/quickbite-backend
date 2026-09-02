package com.quickbite.quickbite.common.exception;

import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.exception.MaxSessionException;
import com.quickbite.quickbite.cart.exception.CartConflictException;
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

    @ExceptionHandler(CartConflictException.class)
    public ProblemDetail handleCartConflictException(CartConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(com.quickbite.quickbite.cart.exception.CartExpiredException.class)
    public ProblemDetail handleCartExpiredException(com.quickbite.quickbite.cart.exception.CartExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Cart Expired");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException.class)
    public ProblemDetail handleRestaurantNotFoundException(com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Restaurant Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.order.exception.OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFoundException(com.quickbite.quickbite.order.exception.OrderNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Order Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.order.exception.OrderStateException.class)
    public ProblemDetail handleOrderStateException(com.quickbite.quickbite.order.exception.OrderStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Invalid Order State");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.payment.exception.PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFoundException(com.quickbite.quickbite.payment.exception.PaymentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Payment Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.allotment.exception.AllotmentNotFoundException.class)
    public ProblemDetail handleAllotmentNotFoundException(com.quickbite.quickbite.allotment.exception.AllotmentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Allotment Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException.class)
    public ProblemDetail handleDeliveryAgentNotFoundException(com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Delivery Agent Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.delivery.exception.NoAvailableDeliveryAgentException.class)
    public ProblemDetail handleNoAvailableDeliveryAgentException(com.quickbite.quickbite.delivery.exception.NoAvailableDeliveryAgentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problemDetail.setTitle("No Delivery Agent Available");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.allotment.exception.AllotmentAlreadyClaimedException.class)
    public ProblemDetail handleAllotmentAlreadyClaimedException(com.quickbite.quickbite.allotment.exception.AllotmentAlreadyClaimedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Allotment Already Claimed");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.allotment.exception.AllotmentAlreadyExistsException.class)
    public ProblemDetail handleAllotmentAlreadyExistsException(com.quickbite.quickbite.allotment.exception.AllotmentAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Allotment Already Exists");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.review.exception.ReviewNotFoundException.class)
    public ProblemDetail handleReviewNotFoundException(com.quickbite.quickbite.review.exception.ReviewNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Review Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.review.exception.DuplicateReviewException.class)
    public ProblemDetail handleDuplicateReviewException(com.quickbite.quickbite.review.exception.DuplicateReviewException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Duplicate Review");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(com.quickbite.quickbite.review.exception.InvalidReviewException.class)
    public ProblemDetail handleInvalidReviewException(com.quickbite.quickbite.review.exception.InvalidReviewException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Review Request");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler({
            org.springframework.dao.OptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ProblemDetail handleOptimisticLockingFailure(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Concurrent Modification Conflict");
        problemDetail.setDetail("The resource was modified by another operation. Please retry.");
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
