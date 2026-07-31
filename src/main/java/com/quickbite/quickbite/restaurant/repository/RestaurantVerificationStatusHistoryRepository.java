package com.quickbite.quickbite.restaurant.repository;

import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RestaurantVerificationStatusHistoryRepository extends JpaRepository<RestaurantVerificationStatusHistory, UUID> {
}
