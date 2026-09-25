package com.quickbite.quickbite.vehicle.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "vehicle_ownership_status_history")
public class VehicleOwnershipStatusHistory extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private VehicleOwnership vehicleOwnership;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "ownership_status", nullable = false)
    private OwnershipStatus status;

    @ManyToOne
    @JoinColumn
    private User reviewedBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
