package com.quickbite.quickbite.vehicle.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
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
public class AdminVehicleServiceImpl implements AdminVehicleService {

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final UserRepository userRepository;
    private final VehicleOwnershipRepository vehicleOwnershipRepository;
    private final VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;
    private final VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;

    public AdminVehicleServiceImpl(
            DeliveryAgentRepository deliveryAgentRepository,
            UserRepository userRepository,
            VehicleOwnershipRepository vehicleOwnershipRepository,
            VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository,
            VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository
    ) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.userRepository = userRepository;
        this.vehicleOwnershipRepository = vehicleOwnershipRepository;
        this.vehicleOwnershipDocumentRepository = vehicleOwnershipDocumentRepository;
        this.vehicleOwnershipStatusHistoryRepository = vehicleOwnershipStatusHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleSummaryResponse> getVehiclesForAgent(UUID deliveryAgentId) {
        DeliveryAgent agent = deliveryAgentRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent not found: " + deliveryAgentId));

        UUID currentVehicleId = agent.getCurrentVehicle() != null ? agent.getCurrentVehicle().getId() : null;

        return vehicleOwnershipRepository.findByOwner(agent).stream()
                .map(ownership -> VehicleSummaryResponse.from(ownership, currentVehicleId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleDetailResponse getVehicleOwnershipDetail(UUID ownershipId) {
        VehicleOwnership ownership = loadVehicleOwnership(ownershipId);
        DeliveryAgent owner = ownership.getOwner();
        UUID currentVehicleId = (owner != null && owner.getCurrentVehicle() != null)
                ? owner.getCurrentVehicle().getId()
                : null;

        List<VehicleOwnershipDocument> documents = vehicleOwnershipDocumentRepository.findByVehicleOwnership(ownership);
        List<VehicleOwnershipStatusHistory> history = vehicleOwnershipStatusHistoryRepository
                .findByVehicleOwnershipOrderByCreatedAtDesc(ownership);

        return VehicleDetailResponse.from(ownership, currentVehicleId, documents, history);
    }

    @Override
    public VehicleDetailResponse expireVehicleOwnership(UUID ownershipId, UUID adminUserId, String remarks) {
        VehicleOwnership ownership = loadVehicleOwnership(ownershipId);

        if (ownership.getCurrentStatus() != OwnershipStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only ACTIVE vehicle ownerships can be marked EXPIRED. Current status: " + ownership.getCurrentStatus()
            );
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        ownership.setCurrentStatus(OwnershipStatus.EXPIRED);
        VehicleOwnership savedOwnership = vehicleOwnershipRepository.save(ownership);

        VehicleOwnershipStatusHistory historyEntry = new VehicleOwnershipStatusHistory();
        historyEntry.setVehicleOwnership(savedOwnership);
        historyEntry.setStatus(OwnershipStatus.EXPIRED);
        historyEntry.setReviewedBy(admin);
        historyEntry.setRemarks(remarks);
        vehicleOwnershipStatusHistoryRepository.save(historyEntry);

        DeliveryAgent owner = savedOwnership.getOwner();
        if (owner != null
                && owner.getCurrentVehicle() != null
                && savedOwnership.getVehicle() != null
                && owner.getCurrentVehicle().getId().equals(savedOwnership.getVehicle().getId())) {
            owner.setCurrentVehicle(null);
            owner.setAvailable(false);
            deliveryAgentRepository.save(owner);
        }

        UUID currentVehicleId = (owner != null && owner.getCurrentVehicle() != null)
                ? owner.getCurrentVehicle().getId()
                : null;
        List<VehicleOwnershipDocument> documents = vehicleOwnershipDocumentRepository.findByVehicleOwnership(savedOwnership);
        List<VehicleOwnershipStatusHistory> history = vehicleOwnershipStatusHistoryRepository
                .findByVehicleOwnershipOrderByCreatedAtDesc(savedOwnership);

        return VehicleDetailResponse.from(savedOwnership, currentVehicleId, documents, history);
    }

    private VehicleOwnership loadVehicleOwnership(UUID ownershipId) {
        return vehicleOwnershipRepository.findById(ownershipId)
                .orElseThrow(() -> new VehicleOwnershipNotFoundException("Vehicle ownership not found: " + ownershipId));
    }
}
