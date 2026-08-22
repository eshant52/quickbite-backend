package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.cuisine.CuisineApprovedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRejectedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRequestedEvent;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.exception.CuisineNotFoundException;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CuisineServiceImpl implements CuisineService {
    private final UserRepository userRepository;
    private final CuisineRepository cuisineRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CuisineServiceImpl(
            UserRepository userRepository,
            CuisineRepository cuisineRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.cuisineRepository = cuisineRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Requests a new cuisine by a restaurant user.
     *
     * @param req the cuisine request containing the name of the cuisine
     * @param requesterId the ID of the user making the request
     * @return the response containing the details of the requested cuisine
     * @throws ResourceNotFoundException if the user with the given ID does not exist
     * @throws ResourceConflictException if a cuisine with the same name already exists
     */
    @Override
    @Transactional
    public CuisineResponse request(CuisineRequest req, UUID requesterId) {
        userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String trimmedName = req.name().trim();

        if (cuisineRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ResourceConflictException("Cuisine with the same name already exists");
        }

        Cuisine cuisine = new Cuisine();
        cuisine.setName(trimmedName);
        cuisine.setStatus(CuisineStatus.PENDING);
        Cuisine savedCuisine = cuisineRepository.save(cuisine);

        kafkaTemplate.send(
                QuickBiteTopics.CUISINE_REQUESTED,
                savedCuisine.getId().toString(),
                new CuisineRequestedEvent(
                        savedCuisine.getId(),
                        savedCuisine.getName(),
                        requesterId,
                        Instant.now()));

        return CuisineResponse.from(savedCuisine);
    }

    // ----------------------------------------------
    // Admin operations
    // ----------------------------------------------

    /**
     * Lists cuisines by their status with cursor-based pagination.
     *
     * @param status the status of the cuisines to list (e.g., PENDING, APPROVED, REJECTED)
     * @param cursor the ID of the last cuisine from the previous page (for pagination)
     * @param size the maximum number of cuisines to return
     * @return a cursor page containing the list of cuisines and pagination information
     */
    @Override
    @Transactional(readOnly = true)
    public CursorPage<CuisineResponse> listByStatus(CuisineStatus status, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        List<Cuisine> cuisines = cuisineRepository.findWithCursor(cursor, status, Limit.of(pageSize + 1));
        return CursorPage.of(
                cuisines.stream().map(CuisineResponse::from).toList(),
                pageSize,
                CuisineResponse::id
        );
    }

    /**
     * Approves a pending cuisine by an admin user.
     *
     * @param cuisineId the ID of the cuisine to approve
     * @param adminId the ID of the admin user approving the cuisine
     * @return the response containing the details of the approved cuisine
     * @throws CuisineNotFoundException if the cuisine or admin user with the given IDs do not exist
     * @throws ResourceConflictException if the cuisine is not in a pending state and cannot be approved
     */
    @Override
    @Transactional
    public CuisineResponse approve(UUID cuisineId, UUID adminId) {
        Cuisine cuisine = cuisineRepository.findById(cuisineId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found"));

        if (cuisine.getStatus() != CuisineStatus.PENDING) {
            throw new ResourceConflictException("Cuisine is not in a pending state and cannot be approved");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        cuisine.setReviewedBy(admin);
        cuisine.setRemarks(null);
        cuisine.setReviewedAt(Instant.now());
        cuisine.setStatus(CuisineStatus.APPROVED);
        Cuisine approvedCuisine = cuisineRepository.save(cuisine);

        kafkaTemplate.send(
                QuickBiteTopics.CUISINE_APPROVED,
                approvedCuisine.getId().toString(),
                new CuisineApprovedEvent(
                        approvedCuisine.getId(),
                        approvedCuisine.getName(),
                        adminId,
                        Instant.now()
                )
        );

        return CuisineResponse.from(approvedCuisine);
    }

    /**
     * Rejects a pending cuisine by an admin user.
     *
     * @param cuisineId the ID of the cuisine to reject
     * @param adminId the ID of the admin user rejecting the cuisine
     * @param remarks the reason for rejecting the cuisine
     * @return the response containing the details of the rejected cuisine
     * @throws CuisineNotFoundException if the cuisine or admin user with the given IDs do not exist
     * @throws ResourceConflictException if the cuisine is not in a pending state and cannot be rejected
     */
    @Override
    @Transactional
    public CuisineResponse reject(UUID cuisineId, UUID adminId, String remarks) {
        Cuisine cuisine = cuisineRepository.findById(cuisineId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found"));

        if (cuisine.getStatus() != CuisineStatus.PENDING) {
            throw new ResourceConflictException("Cuisine is not in a pending state and cannot be rejected");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        cuisine.setRemarks(remarks);
        cuisine.setReviewedBy(admin);
        cuisine.setReviewedAt(Instant.now());
        cuisine.setStatus(CuisineStatus.REJECTED);
        Cuisine rejectedCuisine = cuisineRepository.save(cuisine);

        kafkaTemplate.send(
                QuickBiteTopics.CUISINE_REJECTED,
                rejectedCuisine.getId().toString(),
                new CuisineRejectedEvent(
                        rejectedCuisine.getId(),
                        rejectedCuisine.getName(),
                        adminId,
                        remarks,
                        Instant.now()
                )
        );

        return CuisineResponse.from(rejectedCuisine);
    }

    // ----------------------------------------------
    // Public
    // ----------------------------------------------

    /**
     * Lists all approved cuisines.
     *
     * @return a list of responses containing the details of all approved cuisines
     */
    @Override
    @Transactional(readOnly = true)
    public List<CuisineResponse> listApproved() {
        return cuisineRepository.findByStatus(CuisineStatus.APPROVED).stream()
                .map(CuisineResponse::from)
                .toList();
    }
}
