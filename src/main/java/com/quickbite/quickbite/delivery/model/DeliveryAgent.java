package com.quickbite.quickbite.delivery.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Audited
@Entity
@Table(name = "delivery_agents")
public class DeliveryAgent extends Base {
    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(nullable = true)
    private Vehicle currentVehicle;

    /**
     * Driver-controlled shift/duty status.
     * true = online and ready to accept deliveries; false = off-duty, on break, or on leave.
     */
    @Column(nullable = false)
    private boolean isAvailable;

    /**
     * System-controlled order assignment status.
     * true = currently carrying an active in-flight delivery; false = idle and ready for dispatch.
     */
    @Column(nullable = false)
    private boolean isAssigned;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point lastLocation;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_verification_status", nullable = false)
    private DeliveryAgentVerificationStatus currentStatus;

    @Column
    private Instant lastAssignedAt;

    @OneToMany(mappedBy = "owner")
    @NotAudited
    private List<VehicleOwnership> vehicles;

    @OneToMany(mappedBy = "deliveryAgent")
    @NotAudited
    private List<DeliveryAgentDocument> documents;
}
