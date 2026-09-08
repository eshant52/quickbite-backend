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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AdminCuisineServiceImpl implements AdminCuisineService {

    private final UserRepository userRepository;
    private final CuisineRepository cuisineRepository;
    private final CuisineRequestRepository cuisineRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminCuisineServiceImpl(
            UserRepository userRepository,
            CuisineRepository cuisineRepository,
            CuisineRequestRepository cuisineRequestRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.cuisineRepository = cuisineRepository;
        this.cuisineRequestRepository = cuisineRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<CuisineRequestResponse> listRequestsByStatus(CuisineStatus status, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        var requests = cuisineRequestRepository.findWithCursor(cursor, status, Limit.of(pageSize + 1));
        return CursorPage.of(
                requests.stream().map(CuisineRequestResponse::from).toList(),
                pageSize,
                CuisineRequestResponse::id
        );
    }

    @Override
    public CuisineResponse approve(UUID requestId, UUID adminId) {
        CuisineRequest requestEntity = cuisineRequestRepository.findById(requestId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine request not found"));

        if (requestEntity.getStatus() != CuisineStatus.PENDING) {
            throw new ResourceConflictException("Cuisine request is not in a pending state and cannot be approved");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

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

    @Override
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
}
