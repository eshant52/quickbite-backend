package com.quickbite.quickbite.vehicle.repository;

import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleOwnershipStatusHistoryRepository extends JpaRepository<VehicleOwnershipStatusHistory, UUID> {
    List<VehicleOwnershipStatusHistory> findByVehicleOwnershipOrderByCreatedAtDesc(VehicleOwnership vehicleOwnership);
}
