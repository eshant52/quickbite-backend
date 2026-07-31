package com.quickbite.quickbite.onboarding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record ApplicationHoursRequest(
        @NotEmpty(message = "At least one operating hour entry is required")
        List<@Valid HourEntry> hours
) {
    public record HourEntry(
            @NotNull(message = "Day of week is required") DayOfWeek dayOfWeek,
            @NotNull(message = "Open time is required") LocalTime openTime,
            @NotNull(message = "Close time is required") LocalTime closeTime
    ) {}
}
