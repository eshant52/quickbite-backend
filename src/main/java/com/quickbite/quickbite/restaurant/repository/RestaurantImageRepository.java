package com.quickbite.quickbite.restaurant.repository;

import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantImageRepository extends JpaRepository<RestaurantImage, UUID> {
    List<RestaurantImage> findByRestaurant(Restaurant restaurant);
    Optional<RestaurantImage> findByIdAndRestaurant(UUID id, Restaurant restaurant);
    long countByRestaurant(Restaurant restaurant);
}
