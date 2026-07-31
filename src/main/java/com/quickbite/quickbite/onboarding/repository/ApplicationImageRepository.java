package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.ApplicationImage;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationImageRepository extends JpaRepository<ApplicationImage, UUID> {
    Optional<ApplicationImage> findByIdAndApplication(UUID id, RestaurantApplication application);
    long countByApplication(RestaurantApplication application);
}
