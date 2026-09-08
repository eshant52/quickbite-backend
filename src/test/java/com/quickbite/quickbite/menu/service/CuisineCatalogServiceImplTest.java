package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuisineCatalogServiceImplTest {

    @Mock
    private CuisineRepository cuisineRepository;

    @InjectMocks
    private CuisineCatalogServiceImpl cuisineCatalogService;

    @Test
    @DisplayName("listApproved returns all approved master cuisines mapped to DTOs")
    void listApproved_success() {
        Cuisine c1 = new Cuisine();
        c1.setId(UUID.randomUUID());
        c1.setName("Italian");
        c1.setCreatedAt(Instant.now());

        Cuisine c2 = new Cuisine();
        c2.setId(UUID.randomUUID());
        c2.setName("Mexican");
        c2.setCreatedAt(Instant.now());

        when(cuisineRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CuisineResponse> responses = cuisineCatalogService.listApproved();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("Italian");
        assertThat(responses.get(1).name()).isEqualTo("Mexican");
    }
}
