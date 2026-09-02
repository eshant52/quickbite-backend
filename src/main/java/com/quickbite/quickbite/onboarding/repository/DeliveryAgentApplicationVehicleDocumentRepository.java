package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplicationDocument;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentApplicationVehicleDocumentRepository extends JpaRepository<VehicleApplicationDocument, UUID> {
    List<VehicleApplicationDocument> findByApplicationVehicle(VehicleApplication applicationVehicle);
    Optional<VehicleApplicationDocument> findByApplicationVehicleAndType(VehicleApplication applicationVehicle, VehicleOwnershipDocumentType type);
    boolean existsByApplicationVehicleAndType(VehicleApplication applicationVehicle, VehicleOwnershipDocumentType type);
    void deleteByApplicationVehicleAndType(VehicleApplication applicationVehicle, VehicleOwnershipDocumentType type);
}
