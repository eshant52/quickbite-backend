package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Getter
@Setter
@Entity
public class VehicleOwnershipStatusHistory extends Base {
    @ManyToOne
    private VehicleOwnership vehicleOwnership;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private OwnershipStatus status;

    @ManyToOne
    private User reviewedBy;

    private String review;
}
