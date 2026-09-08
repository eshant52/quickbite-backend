package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.AdminRejectRequest;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.service.AdminCuisineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCuisineControllerTest {

    @Mock
    private AdminCuisineService adminCuisineService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private AdminCuisineController adminCuisineController;

    private UUID adminId;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        requestId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getAdminCuisines returns paginated cuisine requests")
    void getAdminCuisines_success() {
        CuisineRequestResponse mockResponse = new CuisineRequestResponse(
                requestId, "French", CuisineStatus.PENDING, UUID.randomUUID(), "Chef", null, null, null, null, Instant.now()
        );
        CursorPage<CuisineRequestResponse> page = CursorPage.of(List.of(mockResponse), 20, CuisineRequestResponse::id);
        when(adminCuisineService.listRequestsByStatus(CuisineStatus.PENDING, null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<CuisineRequestResponse>> res = adminCuisineController.getAdminCuisines(
                CuisineStatus.PENDING, null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("approveCuisine approves and returns 200 OK")
    void approveCuisine_success() {
        CuisineResponse mockResponse = new CuisineResponse(UUID.randomUUID(), "French", Instant.now());
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(adminCuisineService.approve(requestId, adminId)).thenReturn(mockResponse);

        ResponseEntity<CuisineResponse> res = adminCuisineController.approveCuisine(requestId, jwt);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminCuisineService).approve(requestId, adminId);
    }

    @Test
    @DisplayName("rejectCuisine rejects with remarks and returns 200 OK")
    void rejectCuisine_success() {
        AdminRejectRequest req = new AdminRejectRequest("Not a recognised cuisine");
        CuisineRequestResponse mockResponse = new CuisineRequestResponse(
                requestId, "French", CuisineStatus.REJECTED, UUID.randomUUID(), "Chef", adminId, Instant.now(), "Not a recognised cuisine", null, Instant.now()
        );
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(adminCuisineService.reject(requestId, adminId, "Not a recognised cuisine")).thenReturn(mockResponse);

        ResponseEntity<CuisineRequestResponse> res = adminCuisineController.rejectCuisine(req, requestId, jwt);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminCuisineService).reject(requestId, adminId, "Not a recognised cuisine");
    }
}
