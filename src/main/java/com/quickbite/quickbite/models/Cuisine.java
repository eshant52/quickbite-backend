package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Getter
@Setter
@Entity
public class Cuisine extends Base {
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private CuisineStatus status;

    @ManyToOne
    private User reviewedBy;

    private Instant reviewedAt;
    private String remarks;
}
