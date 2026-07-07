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
@Table(name = "cuisines")
public class Cuisine extends Base {
    @NotBlank(message = "Cuisine name is required")
    @Size(min = 2, max = 100, message = "Cuisine name must be between 2 and 100 characters")
    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "cuisine_status", nullable = false)
    private CuisineStatus status;

    @ManyToOne
    @JoinColumn(nullable = true)
    private User reviewedBy;

    private Instant reviewedAt;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
