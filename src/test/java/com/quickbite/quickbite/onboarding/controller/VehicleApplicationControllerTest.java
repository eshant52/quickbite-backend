package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.vehicle.VehicleApplicationService;
import com.quickbite.quickbite.vehicle.model.VehicleType;
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
class VehicleApplicationControllerTest {

    @Mock private VehicleApplicationService vehicleApplicationService;
    @Mock private AuthenticatedSessionResolver authenticatedSessionResolver;
    @Mock private Jwt jwt;

    @InjectMocks
    private VehicleApplicationController controller;

    private UUID userId;
    private UUID vehicleAppId;
    private VehicleApplicationResponse mockResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vehicleAppId = UUID.randomUUID();

        mockResponse = new VehicleApplicationResponse(
                vehicleAppId,
                UUID.randomUUID(),
                "Driver Dave",
                "VIN999",
                "DL-01-AB-1234",
                VehicleType.SCOOTER,
                "Honda",
                "Activa",
                false,
                ApplicationStatus.DRAFT,
                null,
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("startApplication (standalone) creates vehicle application and returns 201 CREATED")
    void startApplication_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(vehicleApplicationService.startApplication(userId)).thenReturn(mockResponse);

        ResponseEntity<VehicleApplicationResponse> res = controller.startApplication(jwt);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(mockResponse);
        verify(vehicleApplicationService).startApplication(userId);
    }

    @Test
    @DisplayName("submitVehicleApplication submits and returns 200 OK")
    void submitVehicleApplication_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(vehicleApplicationService.submitVehicleApplication(vehicleAppId, userId)).thenReturn(mockResponse);

        ResponseEntity<VehicleApplicationResponse> res = controller.submitVehicleApplication(jwt, vehicleAppId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(vehicleApplicationService).submitVehicleApplication(vehicleAppId, userId);
    }
}
