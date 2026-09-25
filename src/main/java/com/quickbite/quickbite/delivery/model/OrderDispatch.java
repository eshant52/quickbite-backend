package com.quickbite.quickbite.delivery.model;

import com.quickbite.quickbite.common.model.Base;
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
        name = "order_dispatches",
        indexes = {
                @Index(name = "idx_order_dispatches_next_attempt", columnList = "next_attempt_at")
        }
)
public class OrderDispatch extends Base {

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_dispatch_status", nullable = false)
    private DeliveryDispatchStatus status = DeliveryDispatchStatus.NOT_STARTED;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant startedAt;

    @Column(nullable = false)
    private int currentRound = 0;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant nextAttemptAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant dispatchDeadline;

    @Version
    @Column(nullable = false)
    private long version;
}
