package com.quickbite.quickbite.vehicle.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.VehicleType;
import com.quickbite.quickbite.vehicle.service.DeliveryAgentVehicleService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryAgentVehicleController")
class DeliveryAgentVehicleControllerTest {

    @Mock
    private DeliveryAgentVehicleService deliveryAgentVehicleService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @InjectMocks
    private DeliveryAgentVehicleController controller;

    private Jwt jwt;
    private UUID userId;
    private UUID ownershipId;
    private VehicleSummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        userId = UUID.randomUUID();
        ownershipId = UUID.randomUUID();
        summaryResponse = new VehicleSummaryResponse(
                ownershipId,
                UUID.randomUUID(),
                "VIN123456",
                "KA01AB1234",
                VehicleType.BIKE,
                "Honda",
                "Shine",
                OwnershipStatus.ACTIVE,
                true,
                Instant.now()
        );
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
    }

    @Test
    @DisplayName("GET /api/v1/delivery-agent/vehicles returns owned vehicles")
    void getMyVehicles_returnsOk() {
        when(deliveryAgentVehicleService.getMyVehicles(userId)).thenReturn(List.of(summaryResponse));

        ResponseEntity<List<VehicleSummaryResponse>> response = controller.getMyVehicles(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(summaryResponse);
    }

    @Test
    @DisplayName("GET /api/v1/delivery-agent/vehicles/{ownershipId} returns vehicle detail")
    void getMyVehicleDetail_returnsOk() {
        VehicleDetailResponse detail = new VehicleDetailResponse(
                ownershipId, UUID.randomUUID(), UUID.randomUUID(),
                "VIN123456", "KA01AB1234", VehicleType.BIKE, "Honda", "Shine",
                OwnershipStatus.ACTIVE, true, List.of(), List.of(), Instant.now(), Instant.now()
        );
        when(deliveryAgentVehicleService.getMyVehicleDetail(ownershipId, userId)).thenReturn(detail);

        ResponseEntity<VehicleDetailResponse> response = controller.getMyVehicleDetail(jwt, ownershipId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    @DisplayName("PATCH /api/v1/delivery-agent/vehicles/{ownershipId}/select selects current vehicle")
    void selectCurrentVehicle_returnsOk() {
        when(deliveryAgentVehicleService.selectCurrentVehicle(ownershipId, userId)).thenReturn(summaryResponse);

        ResponseEntity<VehicleSummaryResponse> response = controller.selectCurrentVehicle(jwt, ownershipId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryAgentVehicleService).selectCurrentVehicle(ownershipId, userId);
    }

    @Test
    @DisplayName("PATCH /api/v1/delivery-agent/vehicles/{ownershipId}/deactivate deactivates vehicle")
    void deactivateVehicle_returnsOk() {
        when(deliveryAgentVehicleService.deactivateVehicle(ownershipId, userId)).thenReturn(summaryResponse);

        ResponseEntity<VehicleSummaryResponse> response = controller.deactivateVehicle(jwt, ownershipId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryAgentVehicleService).deactivateVehicle(ownershipId, userId);
    }
}
