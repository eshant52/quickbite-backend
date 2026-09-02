package com.quickbite.quickbite.onboarding.model.vehicle;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "vehicle_applications")
public class VehicleApplication extends Base {

    // Populated during initial onboarding (Flow 1)
    @ManyToOne
    @JoinColumn(name = "application_id")
    private DeliveryAgentApplication application;

    // Populated when an already-approved agent adds a new vehicle (Flow 2)
    @ManyToOne
    @JoinColumn(name = "delivery_agent_id")
    private DeliveryAgent deliveryAgent;

    // Lifecycle of the vehicle request
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "application_status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private Instant reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionRemarks;

    // Reused if VIN is already in system
    @ManyToOne
    @JoinColumn(name = "existing_vehicle_id")
    private Vehicle existingVehicle;

    @Column(nullable = false)
    private boolean isOwnershipTransferred = false;

    @Column(length = 30)
    private String vinNumber;

    @Column(length = 20)
    private String numberPlate;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "vehicle_type")
    private VehicleType vehicleType;

    @Column(length = 50)
    private String brand;

    @Column(length = 50)
    private String model;

    @OneToMany(mappedBy = "applicationVehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleApplicationDocument> documents = new ArrayList<>();
}
