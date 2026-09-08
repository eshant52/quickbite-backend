package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.menu.dto.MenuItemImageRequest;
import com.quickbite.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.service.MenuItemManagementService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemOwnerControllerTest {

    @Mock
    private MenuItemManagementService menuItemManagementService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private MenuItemOwnerController menuItemOwnerController;

    private UUID restaurantId;
    private UUID itemId;
    private UUID ownerId;
    private MenuItemResponse sampleResponse;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        sampleResponse = new MenuItemResponse(
                itemId,
                restaurantId,
                "Margherita Pizza",
                "Classic pizza with tomato and mozzarella",
                null,
                new BigDecimal("12.99"),
                "Mains",
                true,
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("createMenuItem resolves owner, delegates to MenuItemManagementService, returns 201 Created")
    void createMenuItem_success() {
        MenuItemRequest request = new MenuItemRequest("Margherita Pizza", "Classic pizza", UUID.randomUUID(), new BigDecimal("12.99"), "Mains", true);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(menuItemManagementService.create(restaurantId, ownerId, request)).thenReturn(sampleResponse);

        ResponseEntity<MenuItemResponse> res = menuItemOwnerController.createMenuItem(jwt, restaurantId, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(sampleResponse);
        verify(menuItemManagementService).create(restaurantId, ownerId, request);
    }

    @Test
    @DisplayName("updateMenuItem resolves owner, delegates to MenuItemManagementService, returns 200 OK")
    void updateMenuItem_success() {
        MenuItemRequest request = new MenuItemRequest("Margherita Pizza", "Updated description", UUID.randomUUID(), new BigDecimal("13.99"), "Mains", true);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(menuItemManagementService.update(restaurantId, itemId, ownerId, request)).thenReturn(sampleResponse);

        ResponseEntity<MenuItemResponse> res = menuItemOwnerController.updateMenuItem(jwt, restaurantId, itemId, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(sampleResponse);
        verify(menuItemManagementService).update(restaurantId, itemId, ownerId, request);
    }

    @Test
    @DisplayName("deleteMenuItem resolves owner, delegates to MenuItemManagementService, returns 204 No Content")
    void deleteMenuItem_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);

        ResponseEntity<Void> res = menuItemOwnerController.deleteMenuItem(jwt, restaurantId, itemId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(menuItemManagementService).delete(restaurantId, itemId, ownerId);
    }

    @Test
    @DisplayName("createMenuItemImage delegates to MenuItemManagementService, returns 201 Created")
    void createMenuItemImage_success() {
        MenuItemImageRequest req = new MenuItemImageRequest("http://example.com/img.jpg", 1);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(menuItemManagementService.addImage(restaurantId, itemId, ownerId, req)).thenReturn(sampleResponse);

        ResponseEntity<MenuItemResponse> res = menuItemOwnerController.createMenuItemImage(jwt, restaurantId, itemId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(sampleResponse);
        verify(menuItemManagementService).addImage(restaurantId, itemId, ownerId, req);
    }

    @Test
    @DisplayName("deleteMenuItemImage delegates to MenuItemManagementService, returns 204 No Content")
    void deleteMenuItemImage_success() {
        UUID imageId = UUID.randomUUID();
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);

        ResponseEntity<Void> res = menuItemOwnerController.deleteMenuItemImage(jwt, restaurantId, itemId, imageId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(menuItemManagementService).removeImage(restaurantId, itemId, imageId, ownerId);
    }
}
