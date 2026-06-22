package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Getter
@Setter
@Entity
public class VehicleOwnershipDocument extends Base {
    @ManyToOne
    private VehicleOwnership vehicleOwnership;

    private String description;
    private String url;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private VehicleOwnershipDocumentType type;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private DocumentVerificationStatus status;

    @ManyToOne
    private User reviewdBy;

    private Instant reviewedAt;
    private String remarks;
}
