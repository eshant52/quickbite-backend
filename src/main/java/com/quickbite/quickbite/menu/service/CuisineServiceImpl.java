package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.cuisine.CuisineApprovedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRejectedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRequestedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CuisineServiceImpl implements CuisineService {

    private final UserRepository userRepository;
    private final CuisineRepository cuisineRepository;
    private final CuisineRequestRepository cuisineRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminAllotmentService adminAllotmentService;

    public CuisineServiceImpl(
            UserRepository userRepository,
            CuisineRepository cuisineRepository,
            CuisineRequestRepository cuisineRequestRepository,
            ApplicationEventPublisher eventPublisher,
            AdminAllotmentService adminAllotmentService) {
        this.userRepository = userRepository;
        this.cuisineRepository = cuisineRepository;
        this.cuisineRequestRepository = cuisineRequestRepository;
        this.eventPublisher = eventPublisher;
        this.adminAllotmentService = adminAllotmentService;
    }

    /**
     * Requests a new cuisine by a restaurant owner.
     */
    @Override
    @Transactional
    public CuisineRequestResponse request(com.quickbite.quickbite.menu.dto.CuisineRequest req, UUID requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String trimmedName = req.name().trim();

        if (cuisineRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ResourceConflictException("Cuisine already exists in master catalog");
        }

        if (cuisineRequestRepository.existsByNameIgnoreCaseAndStatus(trimmedName, CuisineStatus.PENDING)) {
            throw new ResourceConflictException("A pending request for this cuisine already exists");
        }

        CuisineRequest requestEntity = new CuisineRequest();
        requestEntity.setName(trimmedName);
        requestEntity.setRequestedBy(requester);
        requestEntity.setStatus(CuisineStatus.PENDING);
        CuisineRequest savedRequest = cuisineRequestRepository.save(requestEntity);

        // Allot request to workload-balanced admins
        List<AdminAllotment> adminAllotments = adminAllotmentService.allot(savedRequest.getId(), AllotmentReferenceType.CUISINE);

        // Fires after DB transaction commits
        eventPublisher.publishEvent(new CuisineRequestedEvent(
                savedRequest.getId(),
                savedRequest.getName(),
                requesterId,
                adminAllotments.stream()
                        .map(a -> a.getAdmin().getId())
                        .toList(),
                Instant.now()
        ));

        return CuisineRequestResponse.from(savedRequest);
    }

    /**
     * Lists cuisine requests by their status with cursor-based pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPage<CuisineRequestResponse> listRequestsByStatus(CuisineStatus status, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        List<CuisineRequest> requests = cuisineRequestRepository.findWithCursor(cursor, status, Limit.of(pageSize + 1));
        return CursorPage.of(
                requests.stream().map(CuisineRequestResponse::from).toList(),
                pageSize,
                CuisineRequestResponse::id
        );
    }

    /**
     * Approves a pending cuisine request, creates a master Cuisine record, and notifies the requester.
     */
    @Override
    @Transactional
    public CuisineResponse approve(UUID requestId, UUID adminId) {
        CuisineRequest requestEntity = cuisineRequestRepository.findById(requestId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine request not found"));

        if (requestEntity.getStatus() != CuisineStatus.PENDING) {
            throw new ResourceConflictException("Cuisine request is not in a pending state and cannot be approved");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Create or find master catalog item
        Cuisine cuisine = cuisineRepository.findByNameIgnoreCase(requestEntity.getName())
                .orElseGet(() -> {
                    Cuisine c = new Cuisine();
                    c.setName(requestEntity.getName());
                    return cuisineRepository.save(c);
                });

        requestEntity.setStatus(CuisineStatus.APPROVED);
        requestEntity.setReviewedBy(admin);
        requestEntity.setReviewedAt(Instant.now());
        requestEntity.setRemarks(null);
        requestEntity.setCuisine(cuisine);
        cuisineRequestRepository.save(requestEntity);

        // Fires after DB transaction commits
        eventPublisher.publishEvent(new CuisineApprovedEvent(
                requestEntity.getId(),
                cuisine.getId(),
                cuisine.getName(),
                requestEntity.getRequestedBy().getId(),
                adminId,
                Instant.now()
        ));

        return CuisineResponse.from(cuisine);
    }

    /**
     * Rejects a pending cuisine request with remarks and notifies the requester.
     */
    @Override
    @Transactional
    public CuisineRequestResponse reject(UUID requestId, UUID adminId, String remarks) {
        CuisineRequest requestEntity = cuisineRequestRepository.findById(requestId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine request not found"));

        if (requestEntity.getStatus() != CuisineStatus.PENDING) {
            throw new ResourceConflictException("Cuisine request is not in a pending state and cannot be rejected");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        requestEntity.setRemarks(remarks);
        requestEntity.setReviewedBy(admin);
        requestEntity.setReviewedAt(Instant.now());
        requestEntity.setStatus(CuisineStatus.REJECTED);
        CuisineRequest rejectedRequest = cuisineRequestRepository.save(requestEntity);

        // Fires after DB transaction commits
        eventPublisher.publishEvent(new CuisineRejectedEvent(
                rejectedRequest.getId(),
                rejectedRequest.getName(),
                rejectedRequest.getRequestedBy().getId(),
                adminId,
                remarks,
                Instant.now()
        ));

        return CuisineRequestResponse.from(rejectedRequest);
    }

    /**
     * Lists all approved master catalog cuisines.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CuisineResponse> listApproved() {
        return cuisineRepository.findAll().stream()
                .map(CuisineResponse::from)
                .toList();
    }
}
