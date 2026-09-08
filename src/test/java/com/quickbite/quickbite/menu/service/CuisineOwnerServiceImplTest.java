package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.event.cuisine.CuisineRequestedEvent;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import com.quickbite.quickbite.menu.repository.CuisineRequestRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuisineOwnerServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CuisineRepository cuisineRepository;

    @Mock
    private CuisineRequestRepository cuisineRequestRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AdminAllotmentService adminAllotmentService;

    @InjectMocks
    private CuisineOwnerServiceImpl cuisineOwnerService;

    private UUID requesterId;
    private User requester;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        requester = new User();
        requester.setId(requesterId);
        requester.setName("Mario");
    }

    @Test
    @DisplayName("request successfully submits cuisine request, allots to admin, and publishes event")
    void request_success() {
        CuisineRequest req = new CuisineRequest("Ethiopian");

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(cuisineRepository.existsByNameIgnoreCase("Ethiopian")).thenReturn(false);
        when(cuisineRequestRepository.existsByNameIgnoreCaseAndStatus("Ethiopian", CuisineStatus.PENDING)).thenReturn(false);

        com.quickbite.quickbite.menu.model.CuisineRequest savedEntity = new com.quickbite.quickbite.menu.model.CuisineRequest();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName("Ethiopian");
        savedEntity.setRequestedBy(requester);
        savedEntity.setStatus(CuisineStatus.PENDING);

        when(cuisineRequestRepository.save(any(com.quickbite.quickbite.menu.model.CuisineRequest.class))).thenReturn(savedEntity);

        AdminAllotment allotment = new AdminAllotment();
        User admin = new User();
        admin.setId(UUID.randomUUID());
        allotment.setAdmin(admin);

        when(adminAllotmentService.allot(savedEntity.getId(), AllotmentReferenceType.CUISINE)).thenReturn(List.of(allotment));

        CuisineRequestResponse res = cuisineOwnerService.request(req, requesterId);

        assertThat(res.name()).isEqualTo("Ethiopian");
        assertThat(res.status()).isEqualTo(CuisineStatus.PENDING);
        verify(eventPublisher).publishEvent(any(CuisineRequestedEvent.class));
    }

    @Test
    @DisplayName("request throws ResourceNotFoundException if requester not found")
    void request_requesterNotFound() {
        CuisineRequest req = new CuisineRequest("Ethiopian");
        when(userRepository.findById(requesterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuisineOwnerService.request(req, requesterId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("request throws ResourceConflictException if cuisine already exists in master catalog")
    void request_alreadyExistsInCatalog() {
        CuisineRequest req = new CuisineRequest("Italian");
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(cuisineRepository.existsByNameIgnoreCase("Italian")).thenReturn(true);

        assertThatThrownBy(() -> cuisineOwnerService.request(req, requesterId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists in master catalog");
    }

    @Test
    @DisplayName("request throws ResourceConflictException if pending request already exists")
    void request_alreadyPending() {
        CuisineRequest req = new CuisineRequest("Peruvian");
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(cuisineRepository.existsByNameIgnoreCase("Peruvian")).thenReturn(false);
        when(cuisineRequestRepository.existsByNameIgnoreCaseAndStatus("Peruvian", CuisineStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> cuisineOwnerService.request(req, requesterId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already exists");
    }
}
