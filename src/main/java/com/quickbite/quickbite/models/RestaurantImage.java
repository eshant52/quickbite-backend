package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class RestaurantImage extends Base {
    @ManyToOne
    private Restaurant restaurant;

    private String imageUrl;
    private int displayOrder;
}
