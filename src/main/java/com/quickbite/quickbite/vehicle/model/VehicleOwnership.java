package com.quickbite.quickbite.vehicle.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "vehicle_ownerships")
public class VehicleOwnership extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(nullable = false)
    private DeliveryAgent owner;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "ownership_status", nullable = false)
    private OwnershipStatus currentStatus;

    @OneToMany(mappedBy = "vehicleOwnership")
    private List<VehicleOwnershipDocument> documents;
}
