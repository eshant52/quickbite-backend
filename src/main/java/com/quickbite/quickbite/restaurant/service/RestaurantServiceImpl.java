package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.restaurant.dto.RestaurantHoursRequest;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.dto.UpdateRestaurantRequest;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantHours;
import com.quickbite.quickbite.restaurant.model.RestaurantImage;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.repository.RestaurantHoursRepository;
import com.quickbite.quickbite.restaurant.repository.RestaurantImageRepository;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RestaurantServiceImpl implements RestaurantService {

    private static final int MAX_IMAGES = 10;

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantHoursRepository restaurantHoursRepository;
    private final RestaurantImageRepository restaurantImageRepository;

    public RestaurantServiceImpl(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            RestaurantHoursRepository restaurantHoursRepository,
            RestaurantImageRepository restaurantImageRepository
    ) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantHoursRepository = restaurantHoursRepository;
        this.restaurantImageRepository = restaurantImageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<RestaurantSummaryResponse> listMyRestaurants(
            UUID ownerId,
            RestaurantVerificationStatus status,
            UUID cursor,
            int size
    ) {
        int pageSize = Math.clamp(size, 1, 100);
        User owner = loadOwner(ownerId);

        List<Restaurant> restaurants = restaurantRepository
                .findByOwnerWithCursor(owner, status, cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                restaurants.stream()
                        .map(RestaurantSummaryResponse::from)
                        .toList(),
                pageSize,
                RestaurantSummaryResponse::id
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getMyRestaurant(UUID restaurantId, UUID ownerId) {
        User owner = loadOwner(ownerId);
        Restaurant restaurant = loadOwnerRestaurant(restaurantId, owner);

        return RestaurantResponse.from(restaurant);
    }

    @Override
    public RestaurantResponse update(UUID restaurantId, UUID ownerId, UpdateRestaurantRequest req) {
        User owner = loadOwner(ownerId);
        Restaurant restaurant = loadOwnerRestaurant(restaurantId, owner);

        restaurant.setName(req.name());
        restaurant.setDescription(req.description());

        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.from(saved);
    }

    @Override
    public RestaurantResponse setHours(UUID restaurantId, UUID ownerId, List<RestaurantHoursRequest> hours) {
        User owner = loadOwner(ownerId);
        Restaurant restaurant = loadOwnerRestaurant(restaurantId, owner);

        // Clear existing hours in DB
        restaurantHoursRepository.deleteAllByRestaurant(restaurant);

        // Add new hours
        List<RestaurantHours> restaurantHours = hours.stream()
                .map(hour -> {
                    RestaurantHours rh = new RestaurantHours();
                    rh.setRestaurant(restaurant);
                    rh.setDayOfWeek(hour.dayOfWeek());
                    rh.setOpenTime(hour.openTime());
                    rh.setCloseTime(hour.closeTime());
                    return rh;
                })
                .toList();
        List<RestaurantHours> savedHours = restaurantHoursRepository.saveAll(restaurantHours);

        // Sync in-memory collection
        restaurant.setRestaurantHours(savedHours);

        return RestaurantResponse.from(restaurant);
    }

    @Override
    public RestaurantResponse addImage(UUID restaurantId, UUID ownerId, String imageUrl, int displayOrder) {
        User owner = loadOwner(ownerId);
        Restaurant restaurant = loadOwnerRestaurant(restaurantId, owner);

        if (restaurantImageRepository.countByRestaurant(restaurant) >= MAX_IMAGES) {
            throw new BadRequestException("A restaurant cannot have more than " + MAX_IMAGES + " images");
        }

        // Create and save the new image
        RestaurantImage image = new RestaurantImage();
        image.setRestaurant(restaurant);
        image.setImageUrl(imageUrl);
        image.setDisplayOrder(displayOrder);
        RestaurantImage savedImage = restaurantImageRepository.save(image);

        // Sync in-memory collection
        if (restaurant.getRestaurantImages() != null) {
            restaurant.getRestaurantImages().add(savedImage);
        } else {
            List<RestaurantImage> images = new ArrayList<>();
            images.add(savedImage);
            restaurant.setRestaurantImages(images);
        }

        return RestaurantResponse.from(restaurant);
    }

    @Override
    public RestaurantResponse removeImage(UUID restaurantId, UUID ownerId, UUID imageId) {
        User owner = loadOwner(ownerId);
        Restaurant restaurant = loadOwnerRestaurant(restaurantId, owner);

        // Find and delete the image
        RestaurantImage image = restaurantImageRepository.findByIdAndRestaurant(imageId, restaurant)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        restaurantImageRepository.delete(image);

        // Sync in-memory collection
        if (restaurant.getRestaurantImages() != null) {
            restaurant.getRestaurantImages().remove(image);
        }

        return RestaurantResponse.from(restaurant);
    }

    @Override
    public RestaurantResponse toggleClosed(UUID restaurantId, UUID ownerId) {
        User owner = loadOwner(ownerId);
        Restaurant restaurant = loadOwnerRestaurant(restaurantId, owner);

        // Toggle the closed status
        restaurant.setClosed(!restaurant.isClosed());
        Restaurant saved = restaurantRepository.save(restaurant);

        return RestaurantResponse.from(saved);
    }

    // Public facing methods

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));

        if (restaurant.getCurrentStatus() != RestaurantVerificationStatus.APPROVED) {
            throw new RestaurantNotFoundException("Restaurant not found");
        }

        return RestaurantResponse.from(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<RestaurantSummaryResponse> listApproved(UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);

        List<Restaurant> restaurants = restaurantRepository
                .findAllWithCursor(
                        RestaurantVerificationStatus.APPROVED,
                        cursor,
                        Limit.of(pageSize + 1)
                );

        return CursorPage.of(
                restaurants.stream()
                        .map(RestaurantSummaryResponse::from)
                        .toList(),
                pageSize,
                RestaurantSummaryResponse::id
        );
    }

    private User loadOwner(UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
    }

    private Restaurant loadOwnerRestaurant(UUID restaurantId, User owner) {
        return restaurantRepository.findByIdAndOwner(restaurantId, owner)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));
    }
}
