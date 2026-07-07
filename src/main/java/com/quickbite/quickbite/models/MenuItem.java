package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "menu_items")
public class MenuItem extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(length = 100, nullable = false)
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @OneToMany(mappedBy = "menuItem")
    private List<MenuItemImage> images;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Cuisine cuisine;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Price must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Price must be greater than or equal to 0.01")
    private BigDecimal price;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    @Column(length = 50, nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean isAvailable;
}
