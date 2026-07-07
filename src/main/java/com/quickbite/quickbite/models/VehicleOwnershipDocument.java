package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "URL is required")
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
    @JoinColumn(nullable = true)
    private User reviewedBy;

    private Instant reviewedAt;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
