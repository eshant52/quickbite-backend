package com.quickbite.quickbite.user.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Getter
@Setter
@Audited
@Entity
@Table(name = "users")
public class User extends Base {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(length = 100, nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(length = 255, unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
    @Column(length = 20, unique = true, nullable = false)
    private String phoneNumber;

    @NotNull(message = "Password hash is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "user_role", nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean isActive = true;

    private Instant lastLoginAt;
}
