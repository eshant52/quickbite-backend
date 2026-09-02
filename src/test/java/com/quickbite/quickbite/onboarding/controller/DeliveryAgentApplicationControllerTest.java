package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentRequest;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.deliveryagent.DeliveryAgentApplicationService;
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
class DeliveryAgentApplicationControllerTest {

    @Mock private DeliveryAgentApplicationService applicationService;
    @Mock private AuthenticatedSessionResolver authenticatedSessionResolver;
    @Mock private Jwt jwt;

    @InjectMocks
    private DeliveryAgentApplicationController controller;

    private UUID userId;
    private UUID appId;
    private DeliveryAgentApplicationResponse mockResponse;
    private DeliveryAgentApplicationDocumentResponse mockDocResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        appId = UUID.randomUUID();

        mockResponse = new DeliveryAgentApplicationResponse(
                appId,
                userId,
                "Driver Dave",
                "dave@delivery.com",
                "9876543210",
                true,
                true,
                ApplicationStatus.SUBMITTED,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                Instant.now(),
                Instant.now()
        );

        mockDocResponse = new DeliveryAgentApplicationDocumentResponse(
                UUID.randomUUID(),
                DeliveryAgentDocumentType.AADHAR,
                "https://s3.amazonaws.com/aadhar.jpg"
        );
    }

    @Test
    @DisplayName("startApplication creates new application and returns 201 CREATED")
    void startApplication_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(applicationService.startApplication(userId)).thenReturn(mockResponse);

        ResponseEntity<DeliveryAgentApplicationResponse> res = controller.startApplication(jwt);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("addDocument delegates to service and returns 201 CREATED")
    void addDocument_success() {
        DeliveryAgentApplicationDocumentRequest req = new DeliveryAgentApplicationDocumentRequest(
                DeliveryAgentDocumentType.AADHAR, "https://s3.amazonaws.com/aadhar.jpg");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(applicationService.addDocument(appId, userId, req)).thenReturn(mockDocResponse);

        ResponseEntity<DeliveryAgentApplicationDocumentResponse> res = controller.addDocument(jwt, appId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(applicationService).addDocument(appId, userId, req);
    }

    @Test
    @DisplayName("startVehicleApplication returns vehicle application and 201 CREATED")
    void startVehicleApplication_success() {
        VehicleApplicationResponse mockVehicleResponse = new VehicleApplicationResponse(
                UUID.randomUUID(), null, null, null, null, null, null, null,
                false, ApplicationStatus.DRAFT, null, null, null, List.of(),
                Instant.now(), Instant.now());
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(applicationService.startVehicleApplication(appId, userId)).thenReturn(mockVehicleResponse);

        ResponseEntity<VehicleApplicationResponse> res = controller.startVehicleApplication(jwt, appId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(applicationService).startVehicleApplication(appId, userId);
    }

    @Test
    @DisplayName("submitApplication submits application and returns 200 OK")
    void submitApplication_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(applicationService.submitApplication(appId, userId)).thenReturn(mockResponse);

        ResponseEntity<DeliveryAgentApplicationResponse> res = controller.submitApplication(jwt, appId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(applicationService).submitApplication(appId, userId);
    }
}
