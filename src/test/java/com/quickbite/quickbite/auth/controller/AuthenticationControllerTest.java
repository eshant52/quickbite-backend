package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.LoginRequest;
import com.quickbite.quickbite.auth.dto.RefreshRequest;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.AuthenticationService;
import com.quickbite.quickbite.auth.util.DeviceInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private DeviceInfoResolver deviceInfoResolver;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthenticationController controller;

    private DeviceInfo webDeviceInfo;
    private DeviceInfo mobileDeviceInfo;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        webDeviceInfo = new DeviceInfo("Chrome", "Mac", "14.0", ClientType.WEB_BROWSER, "127.0.0.1", "Mozilla/5.0");
        mobileDeviceInfo = new DeviceInfo("QuickBiteApp", "Android", "14", ClientType.MOBILE_APP, "127.0.0.1", "QuickBite/1.0");
        authResponse = new AuthResponse("access-token-123", "refresh-token-456", 900L, UUID.randomUUID());
    }

    @Test
    @DisplayName("login - web browser client sets HttpOnly cookie")
    void login_webBrowser_success() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "Password@123");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-token-456").build();

        when(deviceInfoResolver.resolve(any())).thenReturn(webDeviceInfo);
        when(authenticationService.login(loginRequest, webDeviceInfo)).thenReturn(authResponse);
        when(authCookieService.refreshCookie("refresh-token-456")).thenReturn(cookie);

        ResponseEntity<AuthResponse> response = controller.login(loginRequest, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token-123");
        assertThat(response.getBody().refreshToken()).isNull(); // null in body for web browser
        assertThat(response.getHeaders().getFirst("Set-Cookie")).isNotNull();
    }

    @Test
    @DisplayName("login - mobile app client includes refresh token in body")
    void login_mobileApp_success() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "Password@123");

        when(deviceInfoResolver.resolve(any())).thenReturn(mobileDeviceInfo);
        when(authenticationService.login(loginRequest, mobileDeviceInfo)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = controller.login(loginRequest, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token-123");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token-456");
    }

    @Test
    @DisplayName("refreshToken - mobile app client successfully rotates token")
    void refreshToken_mobileApp_success() {
        RefreshRequest refreshReq = new RefreshRequest("refresh-token-456");

        when(deviceInfoResolver.resolve(any())).thenReturn(mobileDeviceInfo);
        when(authenticationService.refresh("refresh-token-456")).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = controller.refreshToken(refreshReq, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token-123");
        verify(authenticationService).refresh("refresh-token-456");
    }
}
