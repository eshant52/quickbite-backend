package com.quickbite.quickbite.onboarding.model.restaurant;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
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
@Table(name = "restaurant_application_documents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "type"}))
public class RestaurantApplicationDocument extends Base {

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private RestaurantApplication application;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "restaurant_document_type", nullable = false)
    private RestaurantDocumentType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;
}
