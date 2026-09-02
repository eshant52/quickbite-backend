package com.quickbite.quickbite.vehicle.repository;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleOwnershipRepository extends JpaRepository<VehicleOwnership, UUID> {
    List<VehicleOwnership> findByOwner(DeliveryAgent owner);
    Optional<VehicleOwnership> findByIdAndOwner(UUID id, DeliveryAgent owner);
    Optional<VehicleOwnership> findByVehicleAndCurrentStatus(Vehicle vehicle, OwnershipStatus status);
}
