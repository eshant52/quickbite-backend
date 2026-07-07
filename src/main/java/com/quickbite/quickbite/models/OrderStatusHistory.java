package com.quickbite.quickbite.models;

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
@Table(name = "order_status_history")
public class OrderStatusHistory extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "order_status", nullable = false)
    private OrderStatus orderStatus;
}
