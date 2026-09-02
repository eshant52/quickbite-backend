package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantVerificationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RestaurantVerificationStatusHistoryRepository extends JpaRepository<RestaurantVerificationStatusHistory, UUID> {
}
