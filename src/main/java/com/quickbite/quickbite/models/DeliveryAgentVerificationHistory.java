package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Getter
@Setter
@Entity
public class DeliveryAgentVerificationHistory extends Base {
    @ManyToOne
    private DeliveryAgent deliveryAgent;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private DeliveryAgentVerificationStatus status;

    @ManyToOne
    private User reviewedBy;

    private String remarks;
}
