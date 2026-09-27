package com.quickbite.quickbite.delivery.model;

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
@Table(name = "delivery_agent_documents")
public class DeliveryAgentDocument extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private DeliveryAgent deliveryAgent;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_document_type", nullable = false)
    private DeliveryAgentDocumentType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "document_verification_status", nullable = false)
    private DocumentVerificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private Instant reviewedAt;

    @ManyToOne
    @JoinColumn
    private User reviewedBy;
}
