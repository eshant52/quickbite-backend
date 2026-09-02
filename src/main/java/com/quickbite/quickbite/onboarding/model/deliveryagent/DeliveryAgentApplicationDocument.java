package com.quickbite.quickbite.onboarding.model.deliveryagent;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
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
@Table(name = "delivery_agent_application_documents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "type"}))
public class DeliveryAgentApplicationDocument extends Base {

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private DeliveryAgentApplication application;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_document_type", nullable = false)
    private DeliveryAgentDocumentType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;
}
