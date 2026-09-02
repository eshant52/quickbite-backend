package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplicationImage;
import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationImageRepository extends JpaRepository<RestaurantApplicationImage, UUID> {
    List<RestaurantApplicationImage> findByApplicationOrderByDisplayOrderAsc(RestaurantApplication application);
    List<RestaurantApplicationImage> findByApplication(RestaurantApplication application);
    Optional<RestaurantApplicationImage> findByIdAndApplication(UUID id, RestaurantApplication application);
    long countByApplication(RestaurantApplication application);
}
