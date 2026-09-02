package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplicationHours;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record RestaurantApplicationHoursResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
) {
    public static RestaurantApplicationHoursResponse from(RestaurantApplicationHours h) {
        return new RestaurantApplicationHoursResponse(h.getId(), h.getDayOfWeek(), h.getOpenTime(), h.getCloseTime());
    }
}
