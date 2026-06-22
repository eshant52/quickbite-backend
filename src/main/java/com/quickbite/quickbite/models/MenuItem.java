package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
public class MenuItem extends Base {
    @ManyToOne
    private Restaurant restaurant;

    private String name;
    private String description;

    @OneToMany(mappedBy = "menuItem")
    private List<MenuItemImage> images;

    @ManyToOne
    private Cuisine cuisine;

    private BigDecimal price;
    private String category;
    private boolean isAvailable;
}
