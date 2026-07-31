package com.quickbite.quickbite.onboarding.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "restaurant_applications")
public class RestaurantApplication extends Base {

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Step 1: Basic details
    @Column(length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Step 2: Address (embedded — Address row created only on approval)
    @Column(length = 150)
    private String addressStreet;

    @Column(length = 50)
    private String addressCity;

    @Column(length = 50)
    private String addressState;

    @Column(length = 50)
    private String addressCountry;

    @Column(length = 10)
    private String addressPostalCode;

    @Column(length = 20)
    private String addressHouseNumber;

    @Column(length = 100)
    private String addressBuildingName;

    @Column(length = 100)
    private String addressLandmark;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point addressLocation;

    // Step completion flags
    @Column(nullable = false)
    private boolean detailsComplete = false;

    @Column(nullable = false)
    private boolean addressComplete = false;

    @Column(nullable = false)
    private boolean hoursComplete = false;

    @Column(nullable = false)
    private boolean imagesComplete = false;

    @Column(nullable = false)
    private boolean documentsComplete = false;

    // Lifecycle
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "application_status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @ManyToOne
    @JoinColumn(nullable = true)
    private User reviewedBy;

    private Instant reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionRemarks;

    // Set when APPROVED — points to the newly promoted Restaurant row
    @OneToOne
    @JoinColumn(nullable = true)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationHours> hours = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationDocument> documents = new ArrayList<>();
}
