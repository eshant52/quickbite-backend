package com.quickbite.quickbite.onboarding.model.deliveryagent;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.user.model.User;
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
    private DeliveryAgentApplication application;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_verification_status", nullable = false)
    private DeliveryAgentVerificationStatus status;

    @ManyToOne
    private User reviewedBy;

    @Size(max = 500, message = "Remarks must be at most 500 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
