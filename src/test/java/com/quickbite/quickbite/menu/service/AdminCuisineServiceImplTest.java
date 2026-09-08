package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.cuisine.CuisineApprovedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRejectedEvent;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.exception.CuisineNotFoundException;
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
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCuisineServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CuisineRepository cuisineRepository;

    @Mock
    private CuisineRequestRepository cuisineRequestRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminCuisineServiceImpl adminCuisineService;

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
    @DisplayName("listRequestsByStatus()")
    class ListRequestsByStatusTests {

        @Test
        @DisplayName("Returns paginated cuisine requests by status")
        void listRequestsByStatus_success() {
            CuisineRequest req = new CuisineRequest();
            req.setId(UUID.randomUUID());
            req.setName("Thai");
            req.setStatus(CuisineStatus.PENDING);
            req.setRequestedBy(requester);

            when(cuisineRequestRepository.findWithCursor(isNull(), eq(CuisineStatus.PENDING), eq(Limit.of(21))))
                    .thenReturn(List.of(req));

            CursorPage<CuisineRequestResponse> page = adminCuisineService.listRequestsByStatus(
                    CuisineStatus.PENDING, null, 20);

            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).name()).isEqualTo("Thai");
            assertThat(page.hasMore()).isFalse();
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

            CuisineResponse res = adminCuisineService.approve(requestId, adminId);

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

            assertThatThrownBy(() -> adminCuisineService.approve(requestId, adminId))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("not in a pending state");
        }

        @Test
        @DisplayName("Throws CuisineNotFoundException when request does not exist")
        void approve_requestNotFound() {
            UUID requestId = UUID.randomUUID();
            when(cuisineRequestRepository.findById(requestId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminCuisineService.approve(requestId, adminId))
                    .isInstanceOf(CuisineNotFoundException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when admin does not exist")
        void approve_adminNotFound() {
            UUID requestId = UUID.randomUUID();
            CuisineRequest request = new CuisineRequest();
            request.setId(requestId);
            request.setStatus(CuisineStatus.PENDING);

            when(cuisineRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(userRepository.findById(adminId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminCuisineService.approve(requestId, adminId))
                    .isInstanceOf(ResourceNotFoundException.class);
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

            CuisineRequestResponse res = adminCuisineService.reject(requestId, adminId, "Not a valid cuisine category");

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

        @Test
        @DisplayName("Throws exception when request is not in PENDING state")
        void reject_notPending() {
            UUID requestId = UUID.randomUUID();
            CuisineRequest request = new CuisineRequest();
            request.setId(requestId);
            request.setStatus(CuisineStatus.APPROVED);

            when(cuisineRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> adminCuisineService.reject(requestId, adminId, "Remarks"))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("not in a pending state");
        }
    }
}
