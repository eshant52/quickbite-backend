package com.quickbite.quickbite.auth.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
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
    private RefreshTokenFamily family;

    @NotNull(message = "Token hash is required")
    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private int generation = 1;

    // null = this is the current live token; populated when rotated
    private Instant usedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    // null = active; set on hard revocation (breach / logout)
    private Instant revokedAt;
}
