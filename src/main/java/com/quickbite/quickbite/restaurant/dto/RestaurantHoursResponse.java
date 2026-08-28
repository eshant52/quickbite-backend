package com.quickbite.quickbite.restaurant.dto;

import com.quickbite.quickbite.restaurant.model.RestaurantHours;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record RestaurantHoursResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime) {

    public static RestaurantHoursResponse from(RestaurantHours restaurantHours) {
        return new RestaurantHoursResponse(
                restaurantHours.getId(),
                restaurantHours.getDayOfWeek(),
                restaurantHours.getOpenTime(),
                restaurantHours.getCloseTime()
        );
    }
}
