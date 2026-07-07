package com.quickbite.quickbite.models;

import jakarta.persistence.*;
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
@Table(name = "delivery_agent_verification_history")
public class DeliveryAgentVerificationHistory extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private DeliveryAgent deliveryAgent;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_verification_status", nullable = false)
    private DeliveryAgentVerificationStatus status;

    @ManyToOne
    @JoinColumn(nullable = true)
    private User reviewedBy;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
