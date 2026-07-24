package com.quickbite.quickbite.menu.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.menu.model.MenuItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Image URL is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
