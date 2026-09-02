package com.quickbite.quickbite.onboarding.model.deliveryagent;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "delivery_agent_applications")
public class DeliveryAgentApplication extends Base {

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    // Step 1: Personal Identity Documents
    @Column(nullable = false)
    private boolean documentsComplete = false;

    // Step 2: Vehicle & Vehicle Documents
    @Column(nullable = false)
    private boolean vehicleComplete = false;

    // Lifecycle
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "application_status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private Instant reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionRemarks;

    // Promoted reference created upon approval
    @OneToOne
    @JoinColumn(name = "delivery_agent_id")
    private DeliveryAgent deliveryAgent;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryAgentApplicationDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehicleApplication> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "application")
    private List<DeliveryAgentVerificationHistory> verificationHistory = new ArrayList<>();
}
