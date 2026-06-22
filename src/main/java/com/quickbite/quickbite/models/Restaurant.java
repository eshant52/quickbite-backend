package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
public class Restaurant extends Base {
    @ManyToOne
    private User owner;

    private String name;
    private String description;

    @OneToOne
    private Address address;

    private BigDecimal avgRating;
    private int totalRating;

    private boolean isClosed;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private RestaurantVerificationStatus currentStatus;

    @OneToMany(mappedBy = "restaurant")
    private List<MenuItem> menuItems;

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantImage> restaurantImages;

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantHours> restaurantHours;

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantDocument> documents;

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantVerificationStatusHistory> statusHistory;
}
