package com.quickbite.quickbite.vehicle.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "vehicles")
public class Vehicle extends Base {

    @NotBlank(message = "VIN / Chassis number is required")
    @Size(min = 6, max = 30, message = "VIN / Chassis number must be between 6 and 30 characters")
    @Column(name = "vin_number", length = 30, nullable = false, unique = true)
    private String vinNumber;

    @NotBlank(message = "Number plate is required")
    @Size(min = 3, max = 20, message = "Number plate must be between 3 and 20 characters")
    @Column(length = 20, nullable = false)
    private String numberPlate;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String model;
}
