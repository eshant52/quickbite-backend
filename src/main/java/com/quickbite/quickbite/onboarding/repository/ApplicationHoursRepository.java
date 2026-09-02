package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplicationHours;
import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationHoursRepository extends JpaRepository<RestaurantApplicationHours, UUID> {
    List<RestaurantApplicationHours> findByApplication(RestaurantApplication application);
    void deleteAllByApplication(RestaurantApplication application);
}
