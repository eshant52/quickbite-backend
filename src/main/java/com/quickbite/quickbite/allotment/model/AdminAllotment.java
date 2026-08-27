package com.quickbite.quickbite.allotment.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "admin_allotments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_allotment_admin_reference",
                columnNames = {"admin_id", "reference_id"}
        )
)
public class AdminAllotment extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "allotment_reference_type", nullable = false)
    private AllotmentReferenceType referenceType;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "allotment_status", nullable = false)
    private AllotmentStatus status;

    @Column(nullable = false)
    private Instant notifiedAt;

    @Column
    private Instant respondedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
