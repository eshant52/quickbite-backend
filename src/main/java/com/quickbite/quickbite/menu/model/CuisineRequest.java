package com.quickbite.quickbite.menu.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
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

/**
 * Workflow entity representing a restaurant owner's request to add a new cuisine to the platform.
 * <p>
 * If approved by an admin, a new row is created in the {@link Cuisine} master catalog
 * and linked via {@code cuisine}.
 */
@Getter
@Setter
@Entity
@Table(name = "cuisine_requests")
public class CuisineRequest extends Base {

    @NotBlank(message = "Cuisine name is required")
    @Size(min = 2, max = 100, message = "Cuisine name must be between 2 and 100 characters")
    @Column(length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "cuisine_status", nullable = false)
    private CuisineStatus status = CuisineStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private Instant reviewedAt;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuisine_id")
    private Cuisine cuisine;
}
