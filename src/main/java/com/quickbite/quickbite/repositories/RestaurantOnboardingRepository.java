package com.quickbite.quickbite.repositories;

import com.quickbite.quickbite.models.RestaurantOnboardingApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RestaurantOnboardingRepository extends JpaRepository<RestaurantOnboardingApplication, UUID> {
    List<RestaurantOnboardingApplication> findByStatus(RestaurantOnboardingApplication.ApplicationStatus status);
}
