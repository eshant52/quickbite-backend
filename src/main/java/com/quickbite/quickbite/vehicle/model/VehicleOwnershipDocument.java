package com.quickbite.quickbite.vehicle.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "vehicle_ownership_documents")
public class VehicleOwnershipDocument extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private VehicleOwnership vehicleOwnership;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "vehicle_ownership_document_type", nullable = false)
    private VehicleOwnershipDocumentType type;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "document_verification_status", nullable = false)
    private DocumentVerificationStatus status;

    @ManyToOne
    @JoinColumn
    private User reviewedBy;

    private Instant reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
