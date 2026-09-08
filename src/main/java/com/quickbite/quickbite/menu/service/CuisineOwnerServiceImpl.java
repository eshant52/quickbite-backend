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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CuisineOwnerServiceImpl implements CuisineOwnerService {

    private final UserRepository userRepository;
    private final CuisineRepository cuisineRepository;
    private final CuisineRequestRepository cuisineRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminAllotmentService adminAllotmentService;

    public CuisineOwnerServiceImpl(
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

    @Override
    @Transactional
    public CuisineRequestResponse request(CuisineRequest req, UUID requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String trimmedName = req.name().trim();

        if (cuisineRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new ResourceConflictException("Cuisine already exists in master catalog");
        }

        if (cuisineRequestRepository.existsByNameIgnoreCaseAndStatus(trimmedName, CuisineStatus.PENDING)) {
            throw new ResourceConflictException("A pending request for this cuisine already exists");
        }

        com.quickbite.quickbite.menu.model.CuisineRequest requestEntity = new com.quickbite.quickbite.menu.model.CuisineRequest();
        requestEntity.setName(trimmedName);
        requestEntity.setRequestedBy(requester);
        requestEntity.setStatus(CuisineStatus.PENDING);
        com.quickbite.quickbite.menu.model.CuisineRequest savedRequest = cuisineRequestRepository.save(requestEntity);

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
}
