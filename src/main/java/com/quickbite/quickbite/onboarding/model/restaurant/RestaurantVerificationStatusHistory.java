package com.quickbite.quickbite.onboarding.model.restaurant;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.user.model.User;
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
@Table(name = "restaurant_verification_status_history")
public class RestaurantVerificationStatusHistory extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private RestaurantApplication application;

    @ManyToOne
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "restaurant_verification_status", nullable = false)
    private RestaurantVerificationStatus status;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
