package com.quickbite.quickbite.restaurant.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.menu.model.MenuItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.type.SqlTypes;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Audited
@Entity
@Table(name = "restaurants")
public class Restaurant extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private User owner;

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 200, message = "Restaurant name must be between 2 and 200 characters")
    @Column(length = 200, nullable = false)
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @OneToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(nullable = false)
    private Address address;

    @Column(precision = 3, scale = 2)
    @Digits(integer = 1, fraction = 2, message = "Average rating must have up to 1 digit and 2 decimal places")
    @DecimalMin(value = "0.00", message = "Average rating must be greater than or equal to 0.00")
    @DecimalMax(value = "5.00", message = "Average rating must be less than or equal to 5.00")
    private BigDecimal avgRating;

    @Column(nullable = false)
    private Long totalRating;

    @Column(nullable = false)
    private boolean isClosed;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "restaurant_verification_status", nullable = false)
    private RestaurantVerificationStatus currentStatus;

    @OneToMany(mappedBy = "restaurant")
    private List<MenuItem> menuItems;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<RestaurantImage> restaurantImages;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<RestaurantHours> restaurantHours;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<RestaurantDocument> documents;
}
