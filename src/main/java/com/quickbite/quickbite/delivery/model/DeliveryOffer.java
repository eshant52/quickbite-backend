package com.quickbite.quickbite.delivery.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.order.model.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "delivery_offers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_delivery_offers_order_agent", columnNames = {"order_id", "agent_id"})
        },
        indexes = {
                @Index(name = "idx_delivery_offers_agent_status", columnList = "agent_id, status"),
                @Index(name = "idx_delivery_offers_expires_pending", columnList = "expires_at")
        }
)
public class DeliveryOffer extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private DeliveryAgent agent;

    @Column(nullable = false)
    private int roundNumber;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal radiusKm;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_offer_status", nullable = false)
    private DeliveryOfferStatus status = DeliveryOfferStatus.PENDING;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant offeredAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant expiresAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant respondedAt;
}
