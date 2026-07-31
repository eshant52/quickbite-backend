package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.ApplicationHours;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationHoursRepository extends JpaRepository<ApplicationHours, UUID> {
    void deleteAllByApplication(RestaurantApplication application);
}
