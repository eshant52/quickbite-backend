package com.quickbite.quickbite.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "refresh_tokens")
public class RefreshToken extends Base {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User user;

    private UUID familyId;

    @NotNull(message = "Token hash is required")
    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String tokenHash;

    @NotBlank(message = "Device name is required")
    @Size(max = 100, message = "Device name must be at most 100 characters")
    @Column(length = 100, nullable = false)
    private String deviceName;

    @NotBlank(message = "OS is required")
    @Size(max = 50, message = "OS must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String os;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "client_type", nullable = false)
    private ClientType clientType;

    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant lastUsedAt;
}
