package com.quickbite.quickbite.vehicle.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.model.*;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipDocumentRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVehicleServiceImpl")
class AdminVehicleServiceImplTest {

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleOwnershipRepository vehicleOwnershipRepository;

    @Mock
    private VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;

    @Mock
    private VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;

    @InjectMocks
    private AdminVehicleServiceImpl service;

    private UUID adminId;
    private User admin;
    private DeliveryAgent agent;
    private Vehicle vehicle;
    private VehicleOwnership ownership;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        admin = new User();
        admin.setId(adminId);

        vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setVinNumber("VIN999999");
        vehicle.setNumberPlate("MH12AB9999");
        vehicle.setVehicleType(VehicleType.BIKE);
        vehicle.setBrand("Yamaha");
        vehicle.setModel("FZ");

        agent = new DeliveryAgent();
        agent.setId(UUID.randomUUID());
        agent.setCurrentVehicle(vehicle);
        agent.setAvailable(true);
        agent.setAssigned(false);

        ownership = new VehicleOwnership();
        ownership.setId(UUID.randomUUID());
        ownership.setOwner(agent);
        ownership.setVehicle(vehicle);
        ownership.setCurrentStatus(OwnershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("getVehiclesForAgent returns agent vehicles with current shift flag")
    void getVehiclesForAgent_success() {
        when(deliveryAgentRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(vehicleOwnershipRepository.findByOwner(agent)).thenReturn(List.of(ownership));

        List<VehicleSummaryResponse> result = service.getVehiclesForAgent(agent.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isCurrentShiftVehicle()).isTrue();
    }

    @Test
    @DisplayName("expireVehicleOwnership marks ACTIVE ownership EXPIRED, records admin history, and clears agent currentVehicle")
    void expireVehicleOwnership_success() {
        when(vehicleOwnershipRepository.findById(ownership.getId())).thenReturn(Optional.of(ownership));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(vehicleOwnershipRepository.save(ownership)).thenReturn(ownership);
        when(vehicleOwnershipDocumentRepository.findByVehicleOwnership(ownership)).thenReturn(List.of());
        when(vehicleOwnershipStatusHistoryRepository.findByVehicleOwnershipOrderByCreatedAtDesc(ownership))
                .thenReturn(List.of());

        VehicleDetailResponse response = service.expireVehicleOwnership(
                ownership.getId(), adminId, "Insurance expired"
        );

        assertThat(response.ownershipStatus()).isEqualTo(OwnershipStatus.EXPIRED);
        assertThat(response.isCurrentShiftVehicle()).isFalse();
        assertThat(agent.getCurrentVehicle()).isNull();
        assertThat(agent.isAvailable()).isFalse();
        verify(deliveryAgentRepository).save(agent);

        ArgumentCaptor<VehicleOwnershipStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(VehicleOwnershipStatusHistory.class);
        verify(vehicleOwnershipStatusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getReviewedBy()).isEqualTo(admin);
        assertThat(historyCaptor.getValue().getRemarks()).isEqualTo("Insurance expired");
    }

    @Test
    @DisplayName("expireVehicleOwnership throws BadRequestException if ownership is already EXPIRED")
    void expireVehicleOwnership_alreadyExpired_throws() {
        ownership.setCurrentStatus(OwnershipStatus.EXPIRED);
        when(vehicleOwnershipRepository.findById(ownership.getId())).thenReturn(Optional.of(ownership));

        assertThatThrownBy(() -> service.expireVehicleOwnership(ownership.getId(), adminId, "Duplicate expire"))
                .isInstanceOf(BadRequestException.class);
    }
}
