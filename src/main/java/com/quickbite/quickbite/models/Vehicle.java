package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Getter
@Setter
@Entity
public class Vehicle extends Base {
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private VehicleType vehicleType;

    private String numberPlate;
    private String brand;
    private String model;
}
