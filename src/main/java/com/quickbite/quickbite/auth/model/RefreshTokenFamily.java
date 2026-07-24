package com.quickbite.quickbite.auth.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.auth.model.Session;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refresh_token_families")
public class RefreshTokenFamily extends Base {

    @ManyToOne
    @JoinColumn(nullable = false)
    private Session session;

    private Instant revokedAt;
    private Instant reuseDetectedAt;
}
