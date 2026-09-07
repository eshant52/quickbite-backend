package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminDeliveryServiceImpl implements AdminDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(AdminDeliveryServiceImpl.class);

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final UserRepository userRepository;

    public AdminDeliveryServiceImpl(
            DeliveryAgentRepository deliveryAgentRepository,
            UserRepository userRepository
    ) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DeliveryAgentResponse suspendAgent(UUID agentId, UUID adminId, String reason) {
        User admin = loadUser(adminId);
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent not found: " + agentId));

        if (agent.getCurrentStatus() == DeliveryAgentVerificationStatus.SUSPENDED) {
            throw new BadRequestException("Delivery agent is already suspended");
        }

        agent.setCurrentStatus(DeliveryAgentVerificationStatus.SUSPENDED);
        agent.setAvailable(false);
        DeliveryAgent saved = deliveryAgentRepository.save(agent);

        log.warn("Delivery agent {} was SUSPENDED by admin {}. Reason: {}", agentId, admin.getId(), reason);

        return DeliveryAgentResponse.from(saved);
    }

    @Override
    public DeliveryAgentResponse reinstateAgent(UUID agentId, UUID adminId) {
        User admin = loadUser(adminId);
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent not found: " + agentId));

        if (agent.getCurrentStatus() != DeliveryAgentVerificationStatus.SUSPENDED) {
            throw new BadRequestException("Only suspended agents can be reinstated. Current status: " + agent.getCurrentStatus());
        }

        agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        DeliveryAgent saved = deliveryAgentRepository.save(agent);

        log.info("Delivery agent {} was REINSTATED to APPROVED by admin {}", agentId, admin.getId());

        return DeliveryAgentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<DeliveryAgentResponse> listAgentsByStatus(
            DeliveryAgentVerificationStatus status,
            UUID cursor,
            int size
    ) {
        int pageSize = Math.clamp(size, 1, 100);
        List<DeliveryAgent> agents = deliveryAgentRepository.findWithCursor(status, cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                agents.stream().map(DeliveryAgentResponse::from).toList(),
                pageSize,
                DeliveryAgentResponse::id
        );
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
