package com.quickbite.quickbite.onboarding.model.vehicle;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
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
@Table(name = "vehicle_application_documents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_vehicle_id", "type"}))
public class VehicleApplicationDocument extends Base {

    @ManyToOne
    @JoinColumn(name = "application_vehicle_id", nullable = false)
    private VehicleApplication applicationVehicle;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "vehicle_ownership_document_type", nullable = false)
    private VehicleOwnershipDocumentType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;
}
