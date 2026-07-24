package com.quickbite.quickbite.restaurant.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "restaurant_hours")
public class RestaurantHours extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ENUM)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Open time is required")
    @Column(nullable = false)
    private LocalTime openTime;

    @NotNull(message = "Close time is required")
    @Column(nullable = false)
    private LocalTime closeTime;
}
