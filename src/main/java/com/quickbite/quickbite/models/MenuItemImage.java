package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class MenuItemImage extends Base {
    @ManyToOne
    private MenuItem menuItem;

    private String imageUrl;
    private int displayOrder;
}
