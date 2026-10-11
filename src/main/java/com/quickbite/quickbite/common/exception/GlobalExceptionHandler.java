package com.quickbite.quickbite.common.exception;

import com.quickbite.quickbite.allotment.exception.AllotmentAlreadyClaimedException;
import com.quickbite.quickbite.allotment.exception.AllotmentAlreadyExistsException;
import com.quickbite.quickbite.allotment.exception.AllotmentNotFoundException;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.exception.MaxSessionException;
import com.quickbite.quickbite.cart.exception.CartConflictException;
import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.exception.NoAvailableDeliveryAgentException;
import com.quickbite.quickbite.delivery.exception.OfferExpiredException;
import com.quickbite.quickbite.onboarding.exception.ApplicationNotFoundException;
import com.quickbite.quickbite.onboarding.exception.ApplicationStateException;

import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;
import com.quickbite.quickbite.menu.exception.CuisineNotFoundException;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.order.exception.OrderExpiredException;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.quickbite.payment.exception.PaymentVerificationException;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.review.exception.DuplicateReviewException;
import com.quickbite.quickbite.review.exception.InvalidReviewException;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.vehicle.exception.VehicleOwnershipNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler({
            org.springframework.web.method.annotation.HandlerMethodValidationException.class,
            jakarta.validation.ConstraintViolationException.class
    })
    public ProblemDetail handleConstraintOrMethodValidationException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "One or more validation errors occurred.");
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

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail handleSpringAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("Authentication Required");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Full authentication is required to access this resource.");
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle("Access Denied");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "You do not have permission to access this resource.");
        return problemDetail;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequestException(BadRequestException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Bad request");
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Malformed Request Body");
        problemDetail.setDetail("The request body is invalid or cannot be parsed.");
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Parameter Type");
        problemDetail.setDetail("Parameter '" + ex.getName() + "' has an invalid value.");
        return problemDetail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Missing Required Parameter");
        problemDetail.setDetail("Required parameter '" + ex.getParameterName() + "' is missing.");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided.");
        return problemDetail;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
        problemDetail.setTitle("Method Not Allowed");
        problemDetail.setDetail("Request method '" + ex.getMethod() + "' is not supported for this endpoint.");
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

    @ExceptionHandler(CartExpiredException.class)
    public ProblemDetail handleCartExpiredException(CartExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Cart Expired");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ProblemDetail handleRestaurantNotFoundException(RestaurantNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Restaurant Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFoundException(OrderNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Order Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(OrderStateException.class)
    public ProblemDetail handleOrderStateException(OrderStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Invalid Order State");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFoundException(PaymentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Payment Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(AllotmentNotFoundException.class)
    public ProblemDetail handleAllotmentNotFoundException(AllotmentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Allotment Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(DeliveryAgentNotFoundException.class)
    public ProblemDetail handleDeliveryAgentNotFoundException(DeliveryAgentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Delivery Agent Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(NoAvailableDeliveryAgentException.class)
    public ProblemDetail handleNoAvailableDeliveryAgentException(NoAvailableDeliveryAgentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problemDetail.setTitle("No Delivery Agent Available");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(OfferExpiredException.class)
    public ProblemDetail handleOfferExpiredException(OfferExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.valueOf(422));
        problemDetail.setTitle("Offer Expired");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleResourceConflictException(ResourceConflictException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Resource Conflict");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(AllotmentAlreadyClaimedException.class)
    public ProblemDetail handleAllotmentAlreadyClaimedException(AllotmentAlreadyClaimedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Allotment Already Claimed");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(AllotmentAlreadyExistsException.class)
    public ProblemDetail handleAllotmentAlreadyExistsException(AllotmentAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Allotment Already Exists");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ProblemDetail handleReviewNotFoundException(ReviewNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Review Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(MenuItemNotFoundException.class)
    public ProblemDetail handleMenuItemNotFoundException(MenuItemNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Menu Item Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(CuisineNotFoundException.class)
    public ProblemDetail handleCuisineNotFoundException(CuisineNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Cuisine Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(VehicleOwnershipNotFoundException.class)
    public ProblemDetail handleVehicleOwnershipNotFoundException(VehicleOwnershipNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Vehicle Ownership Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(NoResourceFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setDetail("No resource found at the requested path.");
        return problemDetail;
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Entity Not Found");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Requested entity was not found.");
        return problemDetail;
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ProblemDetail handleDuplicateReviewException(DuplicateReviewException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Duplicate Review");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(InvalidReviewException.class)
    public ProblemDetail handleInvalidReviewException(InvalidReviewException ex) {
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

    @ExceptionHandler(PaymentVerificationException.class)
    public ProblemDetail handlePaymentVerificationException(PaymentVerificationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Payment Verification Failed");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setDetail("The operation could not be completed due to a database conflict or constraint violation.");
        return problemDetail;
    }

    @ExceptionHandler(OrderExpiredException.class)
    public ProblemDetail handleOrderExpiredException(OrderExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Order Expired");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(RoutingProviderUnavailableException.class)
    public ProblemDetail handleRoutingProviderUnavailableException(RoutingProviderUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problemDetail.setTitle("Routing Service Unavailable");
        problemDetail.setDetail(ex.getMessage() != null ? ex.getMessage() : "Routing provider service is currently unavailable.");
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
