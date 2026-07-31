package com.quickbite.quickbite.onboarding.dto;

import com.quickbite.quickbite.onboarding.model.ApplicationHours;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ApplicationHoursResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
) {
    public static ApplicationHoursResponse from(ApplicationHours h) {
        return new ApplicationHoursResponse(h.getId(), h.getDayOfWeek(), h.getOpenTime(), h.getCloseTime());
    }
}
