package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "restaurant_onboarding_applications")
public class RestaurantOnboardingApplication extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private User applicant;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private Instant submittedAt = Instant.now();

    @ManyToOne
    private User reviewedBy;

    private Instant reviewedAt;

    @Size(max = 1000)
    @Column(columnDefinition = "TEXT")
    private String reviewRemarks;

    public enum ApplicationStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}