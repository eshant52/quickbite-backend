package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Entity
public class RestaurantHours extends Base {
    @ManyToOne
    private Restaurant restaurant;

    private DayOfWeek dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
}
