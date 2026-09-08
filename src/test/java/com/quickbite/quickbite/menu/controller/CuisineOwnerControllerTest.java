package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.service.CuisineOwnerService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuisineOwnerControllerTest {

    @Mock
    private CuisineOwnerService cuisineOwnerService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CuisineOwnerController cuisineOwnerController;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("requestCuisine creates cuisine request and returns 201 Created")
    void requestCuisine_success() {
        CuisineRequest req = new CuisineRequest("Peruvian");
        CuisineRequestResponse response = new CuisineRequestResponse(
                UUID.randomUUID(), "Peruvian", CuisineStatus.PENDING, ownerId, "Chef", null, null, null, null, Instant.now()
        );
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(cuisineOwnerService.request(req, ownerId)).thenReturn(response);

        ResponseEntity<CuisineRequestResponse> res = cuisineOwnerController.requestCuisine(jwt, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().name()).isEqualTo("Peruvian");
        verify(cuisineOwnerService).request(req, ownerId);
    }
}
