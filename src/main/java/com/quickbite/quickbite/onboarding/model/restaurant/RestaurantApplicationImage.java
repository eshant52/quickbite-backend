package com.quickbite.quickbite.onboarding.model.restaurant;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restaurant_application_images")
public class RestaurantApplicationImage extends Base {

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private RestaurantApplication application;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
