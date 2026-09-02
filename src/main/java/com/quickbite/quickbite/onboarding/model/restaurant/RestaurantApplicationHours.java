package com.quickbite.quickbite.onboarding.model.restaurant;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
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
@Table(name = "restaurant_application_hours",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "day_of_week"}))
public class RestaurantApplicationHours extends Base {

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private RestaurantApplication application;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;
}
