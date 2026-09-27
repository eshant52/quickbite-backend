package com.quickbite.quickbite.restaurant.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.menu.model.MenuItem;
import jakarta.persistence.*;
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

    @Column(length = 200, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @OneToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(nullable = false)
    private Address address;

    @Column(precision = 3, scale = 2)
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
