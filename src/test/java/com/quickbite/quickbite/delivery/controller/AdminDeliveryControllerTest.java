package com.quickbite.quickbite.delivery.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.delivery.dto.AdminRejectDeliveryAgentRequest;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.service.DeliveryService;
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
    private DeliveryService deliveryService;

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
        when(deliveryService.listAgentsByStatus(DeliveryAgentVerificationStatus.PENDING, null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<DeliveryAgentResponse>> res = adminDeliveryController.listAgents(
                DeliveryAgentVerificationStatus.PENDING, null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("approveAgent approves agent and returns 200 OK")
    void approveAgent_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(deliveryService.approveAgent(agentId, adminId)).thenReturn(mockAgentResponse);

        ResponseEntity<DeliveryAgentResponse> res = adminDeliveryController.approveAgent(jwt, agentId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryService).approveAgent(agentId, adminId);
    }

    @Test
    @DisplayName("rejectAgent rejects agent with remarks and returns 200 OK")
    void rejectAgent_success() {
        AdminRejectDeliveryAgentRequest req = new AdminRejectDeliveryAgentRequest("Incomplete KYC");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(adminId);
        when(deliveryService.rejectAgent(agentId, adminId, "Incomplete KYC")).thenReturn(mockAgentResponse);

        ResponseEntity<DeliveryAgentResponse> res = adminDeliveryController.rejectAgent(jwt, agentId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryService).rejectAgent(agentId, adminId, "Incomplete KYC");
    }
}
