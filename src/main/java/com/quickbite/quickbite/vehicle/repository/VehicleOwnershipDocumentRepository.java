package com.quickbite.quickbite.vehicle.repository;

import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleOwnershipDocumentRepository extends JpaRepository<VehicleOwnershipDocument, UUID> {
    List<VehicleOwnershipDocument> findByVehicleOwnership(VehicleOwnership vehicleOwnership);
}
