package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
public class Payment extends Base {
    @ManyToOne
    private Order order;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "payment")
    private List<PaymentStatusHistory> statusHistory;

    private String transactionId;
}
