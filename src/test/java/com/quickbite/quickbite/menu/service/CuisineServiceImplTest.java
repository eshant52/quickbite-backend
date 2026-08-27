package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.model.AllotmentStatus;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.event.cuisine.CuisineApprovedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRejectedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRequestedEvent;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.CuisineRequest;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import com.quickbite.quickbite.menu.repository.CuisineRequestRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuisineServiceImplTest {

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
    private CuisineServiceImpl cuisineService;

    private User requester;
    private User admin;
    private UUID requesterId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        requester = new User();
        requester.setId(requesterId);
        requester.setName("Chef Luigi");
        requester.setEmail("luigi@restaurant.com");

        admin = new User();
        admin.setId(adminId);
        admin.setName("Admin Sarah");
        admin.setEmail("admin@quickbite.com");
    }

    @Nested
    @DisplayName("request()")
    class RequestTests {

        @Test
        @DisplayName("Successfully creates a cuisine request and triggers allotment + event")
        void request_success() {
            com.quickbite.quickbite.menu.dto.CuisineRequest dto =
                    new com.quickbite.quickbite.menu.dto.CuisineRequest("Tuscan");

            when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
            when(cuisineRepository.existsByNameIgnoreCase("Tuscan")).thenReturn(false);
            when(cuisineRequestRepository.existsByNameIgnoreCaseAndStatus("Tuscan", CuisineStatus.PENDING)).thenReturn(false);

            CuisineRequest savedEntity = new CuisineRequest();
            savedEntity.setId(UUID.randomUUID());
            savedEntity.setName("Tuscan");
            savedEntity.setRequestedBy(requester);
            savedEntity.setStatus(CuisineStatus.PENDING);
            savedEntity.setCreatedAt(Instant.now());

            when(cuisineRequestRepository.save(any(CuisineRequest.class))).thenReturn(savedEntity);

            AdminAllotment allotment = new AdminAllotment();
            allotment.setId(UUID.randomUUID());
            allotment.setAdmin(admin);
            allotment.setStatus(AllotmentStatus.PENDING);
            when(adminAllotmentService.allot(savedEntity.getId(), AllotmentReferenceType.CUISINE))
                    .thenReturn(List.of(allotment));

            CuisineRequestResponse res = cuisineService.request(dto, requesterId);

            assertThat(res.id()).isEqualTo(savedEntity.getId());
            assertThat(res.name()).isEqualTo("Tuscan");
            assertThat(res.status()).isEqualTo(CuisineStatus.PENDING);
            assertThat(res.requestedById()).isEqualTo(requesterId);

            ArgumentCaptor<CuisineRequestedEvent> eventCaptor = ArgumentCaptor.forClass(CuisineRequestedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            CuisineRequestedEvent event = eventCaptor.getValue();
            assertThat(event.requestId()).isEqualTo(savedEntity.getId());
            assertThat(event.cuisineName()).isEqualTo("Tuscan");
            assertThat(event.requesterId()).isEqualTo(requesterId);
            assertThat(event.allottedAdminIds()).containsExactly(adminId);
        }

        @Test
        @DisplayName("Throws conflict when cuisine already exists in master catalog")
        void request_conflict_catalog() {
            com.quickbite.quickbite.menu.dto.CuisineRequest dto =
                    new com.quickbite.quickbite.menu.dto.CuisineRequest("Italian");
            when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
            when(cuisineRepository.existsByNameIgnoreCase("Italian")).thenReturn(true);

            assertThatThrownBy(() -> cuisineService.request(dto, requesterId))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("already exists in master catalog");
        }

        @Test
        @DisplayName("Throws conflict when pending request exists")
        void request_conflict_pending() {
            com.quickbite.quickbite.menu.dto.CuisineRequest dto =
                    new com.quickbite.quickbite.menu.dto.CuisineRequest("Italian");
            when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
            when(cuisineRepository.existsByNameIgnoreCase("Italian")).thenReturn(false);
            when(cuisineRequestRepository.existsByNameIgnoreCaseAndStatus("Italian", CuisineStatus.PENDING)).thenReturn(true);

            assertThatThrownBy(() -> cuisineService.request(dto, requesterId))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("A pending request for this cuisine already exists");
        }
    }

    @Nested
    @DisplayName("approve()")
    class ApproveTests {

        @Test
        @DisplayName("Successfully approves request and creates master catalog entry")
        void approve_success() {
            UUID requestId = UUID.randomUUID();
            CuisineRequest request = new CuisineRequest();
            request.setId(requestId);
            request.setName("Mexican");
            request.setRequestedBy(requester);
            request.setStatus(CuisineStatus.PENDING);

            when(cuisineRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(cuisineRepository.findByNameIgnoreCase("Mexican")).thenReturn(Optional.empty());

            Cuisine masterCuisine = new Cuisine();
            masterCuisine.setId(UUID.randomUUID());
            masterCuisine.setName("Mexican");
            masterCuisine.setCreatedAt(Instant.now());
            when(cuisineRepository.save(any(Cuisine.class))).thenReturn(masterCuisine);

            CuisineResponse res = cuisineService.approve(requestId, adminId);

            assertThat(res.id()).isEqualTo(masterCuisine.getId());
            assertThat(res.name()).isEqualTo("Mexican");
            assertThat(request.getStatus()).isEqualTo(CuisineStatus.APPROVED);
            assertThat(request.getReviewedBy()).isEqualTo(admin);

            ArgumentCaptor<CuisineApprovedEvent> eventCaptor = ArgumentCaptor.forClass(CuisineApprovedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            CuisineApprovedEvent event = eventCaptor.getValue();
            assertThat(event.requestId()).isEqualTo(requestId);
            assertThat(event.cuisineId()).isEqualTo(masterCuisine.getId());
            assertThat(event.requesterId()).isEqualTo(requesterId);
            assertThat(event.adminId()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("Throws exception when request is not in PENDING state")
        void approve_notPending() {
            UUID requestId = UUID.randomUUID();
            CuisineRequest request = new CuisineRequest();
            request.setId(requestId);
            request.setStatus(CuisineStatus.APPROVED);

            when(cuisineRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> cuisineService.approve(requestId, adminId))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("not in a pending state");
        }
    }

    @Nested
    @DisplayName("reject()")
    class RejectTests {

        @Test
        @DisplayName("Successfully rejects request with remarks and notifies requester")
        void reject_success() {
            UUID requestId = UUID.randomUUID();
            CuisineRequest request = new CuisineRequest();
            request.setId(requestId);
            request.setName("InvalidDish");
            request.setRequestedBy(requester);
            request.setStatus(CuisineStatus.PENDING);

            when(cuisineRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(cuisineRequestRepository.save(any(CuisineRequest.class))).thenAnswer(i -> i.getArgument(0));

            CuisineRequestResponse res = cuisineService.reject(requestId, adminId, "Not a valid cuisine category");

            assertThat(res.status()).isEqualTo(CuisineStatus.REJECTED);
            assertThat(res.remarks()).isEqualTo("Not a valid cuisine category");

            ArgumentCaptor<CuisineRejectedEvent> eventCaptor = ArgumentCaptor.forClass(CuisineRejectedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            CuisineRejectedEvent event = eventCaptor.getValue();
            assertThat(event.requestId()).isEqualTo(requestId);
            assertThat(event.cuisineName()).isEqualTo("InvalidDish");
            assertThat(event.requesterId()).isEqualTo(requesterId);
            assertThat(event.adminId()).isEqualTo(adminId);
            assertThat(event.rejectionRemarks()).isEqualTo("Not a valid cuisine category");
        }
    }
}
