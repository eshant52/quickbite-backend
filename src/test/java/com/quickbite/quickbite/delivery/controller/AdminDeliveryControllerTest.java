package com.quickbite.quickbite.delivery.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.delivery.dto.AdminSuspendDeliveryAgentRequest;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.service.AdminDeliveryService;
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
class AdminDeliveryControllerTest {

    @Mock
    private AdminDeliveryService adminDeliveryService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private AdminDeliveryController adminDeliveryController;

    private UUID adminId;
    private UUID agentId;
    private DeliveryAgentResponse mockAgentResponse;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        agentId = UUID.randomUUID();

        mockAgentResponse = new DeliveryAgentResponse(
                agentId,
                UUID.randomUUID(),
                "Rider Bob",
                "bob@delivery.com",
                "9876543210",
                false,
                false,
                DeliveryAgentVerificationStatus.APPROVED,
                null,
                null,
                Instant.now()
        );
    }

    @Test
    @DisplayName("listAgents returns paginated list of delivery agents")
    void listAgents_success() {
        CursorPage<DeliveryAgentResponse> page = CursorPage.of(List.of(mockAgentResponse), 20, DeliveryAgentResponse::id);
        when(adminDeliveryService.listAgentsByStatus(DeliveryAgentVerificationStatus.PENDING, null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<DeliveryAgentResponse>> res = adminDeliveryController.listAgents(
                DeliveryAgentVerificationStatus.PENDING, null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("suspendAgent suspends agent with reason and returns 200 OK")
    void suspendAgent_success() {
        AdminSuspendDeliveryAgentRequest req = new AdminSuspendDeliveryAgentRequest("Multiple policy violations");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(adminDeliveryService.suspendAgent(agentId, adminId, "Multiple policy violations")).thenReturn(mockAgentResponse);

        ResponseEntity<DeliveryAgentResponse> res = adminDeliveryController.suspendAgent(jwt, agentId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminDeliveryService).suspendAgent(agentId, adminId, "Multiple policy violations");
    }

    @Test
    @DisplayName("reinstateAgent reinstates agent and returns 200 OK")
    void reinstateAgent_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(adminDeliveryService.reinstateAgent(agentId, adminId)).thenReturn(mockAgentResponse);

        ResponseEntity<DeliveryAgentResponse> res = adminDeliveryController.reinstateAgent(jwt, agentId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminDeliveryService).reinstateAgent(agentId, adminId);
    }
}
