package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.service.CuisineCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuisineCatalogControllerTest {

    @Mock
    private CuisineCatalogService cuisineCatalogService;

    @InjectMocks
    private CuisineCatalogController cuisineCatalogController;

    @Test
    @DisplayName("getApprovedCuisines returns list of approved cuisines with 200 OK")
    void getApprovedCuisines_success() {
        CuisineResponse response = new CuisineResponse(UUID.randomUUID(), "Italian", Instant.now());
        when(cuisineCatalogService.listApproved()).thenReturn(List.of(response));

        ResponseEntity<List<CuisineResponse>> res = cuisineCatalogController.getApprovedCuisines();

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(1);
        assertThat(res.getBody().get(0).name()).isEqualTo("Italian");
        verify(cuisineCatalogService).listApproved();
    }
}
