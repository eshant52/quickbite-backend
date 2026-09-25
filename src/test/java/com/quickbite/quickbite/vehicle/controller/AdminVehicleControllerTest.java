package com.quickbite.quickbite.vehicle.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.vehicle.dto.ExpireVehicleOwnershipRequest;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.VehicleType;
import com.quickbite.quickbite.vehicle.service.AdminVehicleService;
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
@DisplayName("AdminVehicleController")
class AdminVehicleControllerTest {

    @Mock
    private AdminVehicleService adminVehicleService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @InjectMocks
    private AdminVehicleController controller;

    @Test
    @DisplayName("GET /api/v1/admin/vehicles/agents/{agentId} returns agent vehicles")
    void getVehiclesForAgent_returnsOk() {
        UUID agentId = UUID.randomUUID();
        VehicleSummaryResponse summary = new VehicleSummaryResponse(
                UUID.randomUUID(), UUID.randomUUID(), "VIN1", "KA01",
                VehicleType.BIKE, "Honda", "Shine", OwnershipStatus.ACTIVE, true, Instant.now()
        );
        when(adminVehicleService.getVehiclesForAgent(agentId)).thenReturn(List.of(summary));

        ResponseEntity<List<VehicleSummaryResponse>> response = controller.getVehiclesForAgent(agentId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(summary);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/vehicles/ownerships/{ownershipId}/expire expires ownership")
    void expireVehicleOwnership_returnsOk() {
        Jwt jwt = mock(Jwt.class);
        UUID adminId = UUID.randomUUID();
        UUID ownershipId = UUID.randomUUID();
        VehicleDetailResponse detail = new VehicleDetailResponse(
                ownershipId, UUID.randomUUID(), UUID.randomUUID(),
                "VIN1", "KA01", VehicleType.BIKE, "Honda", "Shine",
                OwnershipStatus.EXPIRED, false, List.of(), List.of(), Instant.now(), Instant.now()
        );

        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(adminVehicleService.expireVehicleOwnership(ownershipId, adminId, "Insurance expired"))
                .thenReturn(detail);

        ResponseEntity<VehicleDetailResponse> response = controller.expireVehicleOwnership(
                jwt, ownershipId, new ExpireVehicleOwnershipRequest("Insurance expired")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminVehicleService).expireVehicleOwnership(ownershipId, adminId, "Insurance expired");
    }
}
