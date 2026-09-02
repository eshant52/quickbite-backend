package com.quickbite.quickbite.vehicle.repository;

import com.quickbite.quickbite.vehicle.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByVinNumber(String vinNumber);
    Optional<Vehicle> findByNumberPlate(String numberPlate);
    boolean existsByVinNumber(String vinNumber);
}
