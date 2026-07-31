package com.quickbite.quickbite.onboarding.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "application_images")
public class ApplicationImage extends Base {

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private RestaurantApplication application;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
