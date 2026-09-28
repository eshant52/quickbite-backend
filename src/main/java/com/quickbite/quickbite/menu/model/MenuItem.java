package com.quickbite.quickbite.menu.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Audited
@Entity
@Table(name = "menu_items")
public class MenuItem extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @NotAudited
    private List<MenuItemImage> images = new ArrayList<>();

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(nullable = false)
    private Cuisine cuisine;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean isAvailable;

    public void addImage(MenuItemImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
        image.setMenuItem(this);
    }

    public void removeImage(MenuItemImage image) {
        if (this.images == null) return;
        this.images.remove(image);
        image.setMenuItem(null);
    }
}
