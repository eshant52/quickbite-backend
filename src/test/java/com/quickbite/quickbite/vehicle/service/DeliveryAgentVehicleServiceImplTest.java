package com.quickbite.quickbite.vehicle.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.exception.VehicleOwnershipNotFoundException;
import com.quickbite.quickbite.vehicle.model.*;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipDocumentRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryAgentVehicleServiceImpl")
class DeliveryAgentVehicleServiceImplTest {

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private VehicleOwnershipRepository vehicleOwnershipRepository;

    @Mock
    private VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;

    @Mock
    private VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;

    @InjectMocks
    private DeliveryAgentVehicleServiceImpl service;

    private UUID userId;
    private DeliveryAgent agent;
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private VehicleOwnership ownership1;
    private VehicleOwnership ownership2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        vehicle1 = new Vehicle();
        vehicle1.setId(UUID.randomUUID());
        vehicle1.setVinNumber("VIN000001");
        vehicle1.setNumberPlate("KA01AB1234");
        vehicle1.setVehicleType(VehicleType.BIKE);
        vehicle1.setBrand("Honda");
        vehicle1.setModel("Shine");

        vehicle2 = new Vehicle();
        vehicle2.setId(UUID.randomUUID());
        vehicle2.setVinNumber("VIN000002");
        vehicle2.setNumberPlate("KA01CD5678");
        vehicle2.setVehicleType(VehicleType.SCOOTER);
        vehicle2.setBrand("Ather");
        vehicle2.setModel("450X");

        agent = new DeliveryAgent();
        agent.setId(UUID.randomUUID());
        agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        agent.setCurrentVehicle(vehicle1);
        agent.setAvailable(true);
        agent.setAssigned(false);

        ownership1 = new VehicleOwnership();
        ownership1.setId(UUID.randomUUID());
        ownership1.setOwner(agent);
        ownership1.setVehicle(vehicle1);
        ownership1.setCurrentStatus(OwnershipStatus.ACTIVE);
        ownership1.setCreatedAt(Instant.now());

        ownership2 = new VehicleOwnership();
        ownership2.setId(UUID.randomUUID());
        ownership2.setOwner(agent);
        ownership2.setVehicle(vehicle2);
        ownership2.setCurrentStatus(OwnershipStatus.ACTIVE);
        ownership2.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("getMyVehicles")
    class GetMyVehiclesTests {

        @Test
        @DisplayName("Returns all vehicles with isCurrentShiftVehicle flag correctly marked")
        void getMyVehicles_marksCurrentShiftVehicle() {
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByOwner(agent)).thenReturn(List.of(ownership1, ownership2));

            List<VehicleSummaryResponse> result = service.getMyVehicles(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).ownershipId()).isEqualTo(ownership1.getId());
            assertThat(result.get(0).isCurrentShiftVehicle()).isTrue();
            assertThat(result.get(1).ownershipId()).isEqualTo(ownership2.getId());
            assertThat(result.get(1).isCurrentShiftVehicle()).isFalse();
        }

        @Test
        @DisplayName("Throws DeliveryAgentNotFoundException when agent profile missing")
        void getMyVehicles_agentNotFound() {
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMyVehicles(userId))
                    .isInstanceOf(DeliveryAgentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMyVehicleDetail")
    class GetMyVehicleDetailTests {

        @Test
        @DisplayName("Returns vehicle details along with verified documents and status history")
        void getMyVehicleDetail_success() {
            VehicleOwnershipDocument doc = new VehicleOwnershipDocument();
            doc.setId(UUID.randomUUID());
            doc.setVehicleOwnership(ownership1);
            doc.setType(VehicleOwnershipDocumentType.RC);
            doc.setUrl("https://s3.example.com/rc.pdf");
            doc.setStatus(DocumentVerificationStatus.APPROVED);

            VehicleOwnershipStatusHistory history = new VehicleOwnershipStatusHistory();
            history.setId(UUID.randomUUID());
            history.setVehicleOwnership(ownership1);
            history.setStatus(OwnershipStatus.ACTIVE);

            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership1.getId(), agent))
                    .thenReturn(Optional.of(ownership1));
            when(vehicleOwnershipDocumentRepository.findByVehicleOwnership(ownership1))
                    .thenReturn(List.of(doc));
            when(vehicleOwnershipStatusHistoryRepository.findByVehicleOwnershipOrderByCreatedAtDesc(ownership1))
                    .thenReturn(List.of(history));

            VehicleDetailResponse response = service.getMyVehicleDetail(ownership1.getId(), userId);

            assertThat(response.ownershipId()).isEqualTo(ownership1.getId());
            assertThat(response.isCurrentShiftVehicle()).isTrue();
            assertThat(response.documents()).hasSize(1);
            assertThat(response.statusHistory()).hasSize(1);
        }

        @Test
        @DisplayName("Throws VehicleOwnershipNotFoundException if ownership does not belong to agent")
        void getMyVehicleDetail_notFound() {
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership1.getId(), agent))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMyVehicleDetail(ownership1.getId(), userId))
                    .isInstanceOf(VehicleOwnershipNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("selectCurrentVehicle")
    class SelectCurrentVehicleTests {

        @Test
        @DisplayName("Switches currentVehicle to selected ACTIVE vehicle when agent is idle")
        void selectCurrentVehicle_success() {
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership2.getId(), agent))
                    .thenReturn(Optional.of(ownership2));

            VehicleSummaryResponse response = service.selectCurrentVehicle(ownership2.getId(), userId);

            assertThat(agent.getCurrentVehicle()).isEqualTo(vehicle2);
            assertThat(response.isCurrentShiftVehicle()).isTrue();
            verify(deliveryAgentRepository).save(agent);
        }

        @Test
        @DisplayName("Rejects selection if agent is currently carrying an assigned order")
        void selectCurrentVehicle_whenAssigned_throwsBadRequest() {
            agent.setAssigned(true);
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership2.getId(), agent))
                    .thenReturn(Optional.of(ownership2));

            assertThatThrownBy(() -> service.selectCurrentVehicle(ownership2.getId(), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot switch your shift vehicle while carrying an active delivery");
        }

        @Test
        @DisplayName("Rejects selection if ownership is not ACTIVE")
        void selectCurrentVehicle_nonActive_throwsBadRequest() {
            ownership2.setCurrentStatus(OwnershipStatus.EXPIRED);
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership2.getId(), agent))
                    .thenReturn(Optional.of(ownership2));

            assertThatThrownBy(() -> service.selectCurrentVehicle(ownership2.getId(), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only ACTIVE verified vehicles can be selected");
        }
    }

    @Nested
    @DisplayName("deactivateVehicle")
    class DeactivateVehicleTests {

        @Test
        @DisplayName("Deactivates current shift vehicle, clears currentVehicle, and sets agent off-duty")
        void deactivateVehicle_currentVehicle_clearsShiftAndSetsOffline() {
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership1.getId(), agent))
                    .thenReturn(Optional.of(ownership1));
            when(vehicleOwnershipRepository.save(ownership1)).thenReturn(ownership1);

            VehicleSummaryResponse response = service.deactivateVehicle(ownership1.getId(), userId);

            assertThat(response.ownershipStatus()).isEqualTo(OwnershipStatus.EXPIRED);
            assertThat(response.isCurrentShiftVehicle()).isFalse();
            assertThat(agent.getCurrentVehicle()).isNull();
            assertThat(agent.isAvailable()).isFalse();
            verify(deliveryAgentRepository).save(agent);

            ArgumentCaptor<VehicleOwnershipStatusHistory> historyCaptor =
                    ArgumentCaptor.forClass(VehicleOwnershipStatusHistory.class);
            verify(vehicleOwnershipStatusHistoryRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getStatus()).isEqualTo(OwnershipStatus.EXPIRED);
        }

        @Test
        @DisplayName("Rejects deactivating current shift vehicle while carrying an active order")
        void deactivateVehicle_whenAssigned_throwsBadRequest() {
            agent.setAssigned(true);
            when(deliveryAgentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
            when(vehicleOwnershipRepository.findByIdAndOwner(ownership1.getId(), agent))
                    .thenReturn(Optional.of(ownership1));

            assertThatThrownBy(() -> service.deactivateVehicle(ownership1.getId(), userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot deactivate your current shift vehicle while carrying an active delivery");

            verify(vehicleOwnershipRepository, never()).save(any());
        }
    }
}
