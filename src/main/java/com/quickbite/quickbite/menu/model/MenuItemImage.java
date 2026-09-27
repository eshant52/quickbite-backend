package com.quickbite.quickbite.menu.model;

import com.quickbite.quickbite.common.model.Base;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_item_images")
public class MenuItemImage extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private MenuItem menuItem;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
