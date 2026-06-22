package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.util.List;

@Getter
@Setter
@Entity
public class VehicleOwnership extends Base {
    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private DeliveryAgent owner;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private OwnershipStatus currentStatus;

    @OneToMany(mappedBy = "vehicleOwnership")
    private List<VehicleOwnershipDocument> documents;
}
