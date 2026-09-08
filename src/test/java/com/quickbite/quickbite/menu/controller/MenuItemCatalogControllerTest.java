package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.service.MenuItemQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemCatalogControllerTest {

    @Mock
    private MenuItemQueryService menuItemQueryService;

    @InjectMocks
    private MenuItemCatalogController menuItemCatalogController;

    private UUID restaurantId;
    private UUID itemId;
    private MenuItemResponse sampleResponse;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        itemId = UUID.randomUUID();

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
    @DisplayName("getMenuItems delegates to MenuItemQueryService and returns 200 OK")
    void getMenuItems_success() {
        CursorPage<MenuItemResponse> cursorPage = new CursorPage<>(List.of(sampleResponse), null, false, 1);
        when(menuItemQueryService.listByRestaurant(restaurantId, true, null, 20)).thenReturn(cursorPage);

        ResponseEntity<CursorPage<MenuItemResponse>> res = menuItemCatalogController.getMenuItems(restaurantId, null, 20, true);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().content()).hasSize(1);
        assertThat(res.getBody().content().get(0).name()).isEqualTo("Margherita Pizza");
        verify(menuItemQueryService).listByRestaurant(restaurantId, true, null, 20);
    }

    @Test
    @DisplayName("getMenuItem delegates to MenuItemQueryService and returns 200 OK")
    void getMenuItem_success() {
        when(menuItemQueryService.getById(restaurantId, itemId)).thenReturn(sampleResponse);

        ResponseEntity<MenuItemResponse> res = menuItemCatalogController.getMenuItem(restaurantId, itemId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(sampleResponse);
        verify(menuItemQueryService).getById(restaurantId, itemId);
    }
}
