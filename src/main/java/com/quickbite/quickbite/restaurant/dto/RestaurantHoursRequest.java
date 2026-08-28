package com.quickbite.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RestaurantHoursRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime openTime,
        @NotNull LocalTime closeTime
) {
}
