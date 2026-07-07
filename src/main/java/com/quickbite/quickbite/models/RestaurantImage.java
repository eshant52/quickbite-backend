package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restaurant_images")
public class RestaurantImage extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @NotBlank(message = "Image URL is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
