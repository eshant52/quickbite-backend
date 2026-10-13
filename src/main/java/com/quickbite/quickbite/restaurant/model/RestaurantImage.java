package com.quickbite.quickbite.restaurant.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "restaurant_images",
        indexes = {
                @Index(name = "idx_restaurant_images_restaurant_id", columnList = "restaurant_id")
        }
)
public class RestaurantImage extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
