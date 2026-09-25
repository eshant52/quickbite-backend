package com.quickbite.quickbite.vehicle.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.exception.VehicleOwnershipNotFoundException;
import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocument;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipStatusHistory;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipDocumentRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryAgentVehicleServiceImpl implements DeliveryAgentVehicleService {

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final VehicleOwnershipRepository vehicleOwnershipRepository;
    private final VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;
    private final VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;

    public DeliveryAgentVehicleServiceImpl(
            DeliveryAgentRepository deliveryAgentRepository,
            VehicleOwnershipRepository vehicleOwnershipRepository,
            VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository,
            VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository
    ) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.vehicleOwnershipRepository = vehicleOwnershipRepository;
        this.vehicleOwnershipDocumentRepository = vehicleOwnershipDocumentRepository;
        this.vehicleOwnershipStatusHistoryRepository = vehicleOwnershipStatusHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleSummaryResponse> getMyVehicles(UUID agentUserId) {
        DeliveryAgent agent = loadDeliveryAgent(agentUserId);
        UUID currentVehicleId = agent.getCurrentVehicle() != null ? agent.getCurrentVehicle().getId() : null;

        return vehicleOwnershipRepository.findByOwner(agent).stream()
                .map(ownership -> VehicleSummaryResponse.from(ownership, currentVehicleId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleDetailResponse getMyVehicleDetail(UUID ownershipId, UUID agentUserId) {
        DeliveryAgent agent = loadDeliveryAgent(agentUserId);
        VehicleOwnership ownership = loadOwnedVehicleOwnership(ownershipId, agent);
        UUID currentVehicleId = agent.getCurrentVehicle() != null ? agent.getCurrentVehicle().getId() : null;

        List<VehicleOwnershipDocument> documents = vehicleOwnershipDocumentRepository.findByVehicleOwnership(ownership);
        List<VehicleOwnershipStatusHistory> history = vehicleOwnershipStatusHistoryRepository
                .findByVehicleOwnershipOrderByCreatedAtDesc(ownership);

        return VehicleDetailResponse.from(ownership, currentVehicleId, documents, history);
    }

    @Override
    public VehicleSummaryResponse selectCurrentVehicle(UUID ownershipId, UUID agentUserId) {
        DeliveryAgent agent = loadDeliveryAgent(agentUserId);
        VehicleOwnership ownership = loadOwnedVehicleOwnership(ownershipId, agent);

        if (ownership.getCurrentStatus() != OwnershipStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only ACTIVE verified vehicles can be selected for delivery shifts. Current status: "
                            + ownership.getCurrentStatus()
            );
        }

        if (agent.isAssigned()) {
            throw new BadRequestException("Cannot switch your shift vehicle while carrying an active delivery order");
        }

        agent.setCurrentVehicle(ownership.getVehicle());
        deliveryAgentRepository.save(agent);

        return VehicleSummaryResponse.from(ownership, ownership.getVehicle().getId());
    }

    @Override
    public VehicleSummaryResponse deactivateVehicle(UUID ownershipId, UUID agentUserId) {
        DeliveryAgent agent = loadDeliveryAgent(agentUserId);
        VehicleOwnership ownership = loadOwnedVehicleOwnership(ownershipId, agent);

        if (ownership.getCurrentStatus() != OwnershipStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only ACTIVE vehicle ownerships can be deactivated. Current status: " + ownership.getCurrentStatus()
            );
        }

        boolean isCurrentVehicle = agent.getCurrentVehicle() != null
                && ownership.getVehicle() != null
                && agent.getCurrentVehicle().getId().equals(ownership.getVehicle().getId());

        if (isCurrentVehicle && agent.isAssigned()) {
            throw new BadRequestException("Cannot deactivate your current shift vehicle while carrying an active delivery order");
        }

        ownership.setCurrentStatus(OwnershipStatus.EXPIRED);
        VehicleOwnership savedOwnership = vehicleOwnershipRepository.save(ownership);

        VehicleOwnershipStatusHistory history = new VehicleOwnershipStatusHistory();
        history.setVehicleOwnership(savedOwnership);
        history.setStatus(OwnershipStatus.EXPIRED);
        history.setRemarks("Deactivated by delivery agent");
        vehicleOwnershipStatusHistoryRepository.save(history);

        if (isCurrentVehicle) {
            agent.setCurrentVehicle(null);
            if (agent.isAvailable()) {
                agent.setAvailable(false);
            }
            deliveryAgentRepository.save(agent);
        }

        UUID updatedCurrentVehicleId = agent.getCurrentVehicle() != null ? agent.getCurrentVehicle().getId() : null;
        return VehicleSummaryResponse.from(savedOwnership, updatedCurrentVehicleId);
    }

    private DeliveryAgent loadDeliveryAgent(UUID userId) {
        return deliveryAgentRepository.findByUserId(userId)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent profile not found for user: " + userId));
    }

    private VehicleOwnership loadOwnedVehicleOwnership(UUID ownershipId, DeliveryAgent agent) {
        return vehicleOwnershipRepository.findByIdAndOwner(ownershipId, agent)
                .orElseThrow(() -> new VehicleOwnershipNotFoundException("Vehicle ownership not found: " + ownershipId));
    }
}
