package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "delivery_agents")
public class DeliveryAgent extends Base {
    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(nullable = true)
    private Vehicle currentVehicle;

    @Column(nullable = false)
    private boolean isAvailable;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point lastLocation;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_verification_status", nullable = false)
    private DeliveryAgentVerificationStatus currentStatus;

    @OneToMany(mappedBy = "owner")
    private List<VehicleOwnership> vehicles;

    @OneToMany(mappedBy = "deliveryAgent")
    private List<DeliveryAgentDocument> documents;

    @OneToMany(mappedBy = "deliveryAgent")
    private List<DeliveryAgentVerificationHistory> verificationHistory;
}
