package com.quickbite.quickbite.common.exception;

import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;
import com.quickbite.quickbite.menu.exception.CuisineNotFoundException;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.order.exception.OrderExpiredException;
import com.quickbite.quickbite.vehicle.exception.VehicleOwnershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleAccessDeniedException returns 403 Forbidden")
    void testAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ProblemDetail problem = exceptionHandler.handleAccessDeniedException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Access Denied");
        assertThat(problem.getDetail()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("handleSpringAuthenticationException returns 401 Unauthorized")
    void testSpringAuthenticationException() {
        org.springframework.security.authentication.BadCredentialsException ex =
                new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
        ProblemDetail problem = exceptionHandler.handleSpringAuthenticationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getTitle()).isEqualTo("Authentication Required");
        assertThat(problem.getDetail()).isEqualTo("Bad credentials");
    }

    @Test
    @DisplayName("handleDataIntegrityViolationException returns 409 Conflict")
    void testDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key");
        ProblemDetail problem = exceptionHandler.handleDataIntegrityViolationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Data Integrity Violation");
    }

    @Test
    @DisplayName("handleMenuItemNotFoundException returns 404 Not Found")
    void testMenuItemNotFoundException() {
        MenuItemNotFoundException ex = new MenuItemNotFoundException("Menu item not found");
        ProblemDetail problem = exceptionHandler.handleMenuItemNotFoundException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Menu Item Not Found");
        assertThat(problem.getDetail()).isEqualTo("Menu item not found");
    }

    @Test
    @DisplayName("handleCuisineNotFoundException returns 404 Not Found")
    void testCuisineNotFoundException() {
        CuisineNotFoundException ex = new CuisineNotFoundException("Cuisine not found");
        ProblemDetail problem = exceptionHandler.handleCuisineNotFoundException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Cuisine Not Found");
        assertThat(problem.getDetail()).isEqualTo("Cuisine not found");
    }

    @Test
    @DisplayName("handleVehicleOwnershipNotFoundException returns 404 Not Found")
    void testVehicleOwnershipNotFoundException() {
        VehicleOwnershipNotFoundException ex = new VehicleOwnershipNotFoundException("Vehicle ownership not found");
        ProblemDetail problem = exceptionHandler.handleVehicleOwnershipNotFoundException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Vehicle Ownership Not Found");
        assertThat(problem.getDetail()).isEqualTo("Vehicle ownership not found");
    }

    @Test
    @DisplayName("handleRoutingProviderUnavailableException returns 503 Service Unavailable")
    void testRoutingProviderUnavailableException() {
        RoutingProviderUnavailableException ex = new RoutingProviderUnavailableException("OSRM down");
        ProblemDetail problem = exceptionHandler.handleRoutingProviderUnavailableException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Routing Service Unavailable");
        assertThat(problem.getDetail()).isEqualTo("OSRM down");
    }

    @Test
    @DisplayName("handleOrderExpiredException returns 400 Bad Request")
    void testOrderExpiredException() {
        OrderExpiredException ex = new OrderExpiredException(UUID.randomUUID());
        ProblemDetail problem = exceptionHandler.handleOrderExpiredException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Order Expired");
    }

    @Test
    @DisplayName("handleHttpMessageNotReadableException returns 400 Bad Request")
    void testHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON", new MockHttpInputMessage(new byte[0]));
        ProblemDetail problem = exceptionHandler.handleHttpMessageNotReadableException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Malformed Request Body");
    }

    @Test
    @DisplayName("handleMissingServletRequestParameterException returns 400 Bad Request")
    void testMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("page", "int");
        ProblemDetail problem = exceptionHandler.handleMissingServletRequestParameterException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Missing Required Parameter");
        assertThat(problem.getDetail()).contains("page");
    }

    @Test
    @DisplayName("handleHttpRequestMethodNotSupportedException returns 405 Method Not Allowed")
    void testHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ProblemDetail problem = exceptionHandler.handleHttpRequestMethodNotSupportedException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(problem.getTitle()).isEqualTo("Method Not Allowed");
        assertThat(problem.getDetail()).contains("POST");
    }

    @Test
    @DisplayName("handleIllegalArgumentException returns 400 Bad Request")
    void testIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        ProblemDetail problem = exceptionHandler.handleIllegalArgumentException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Argument");
        assertThat(problem.getDetail()).isEqualTo("Invalid argument");
    }
}
