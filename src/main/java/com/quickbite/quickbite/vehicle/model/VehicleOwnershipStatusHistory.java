package com.quickbite.quickbite.vehicle.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
    @JoinColumn(nullable = true)
    private User reviewedBy;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
