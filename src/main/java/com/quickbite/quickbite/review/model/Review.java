package com.quickbite.quickbite.review.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.order.model.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uq_reviews_order_id", columnNames = "order_id"),
        indexes = {
                @Index(name = "idx_reviews_restaurant_id", columnList = "restaurant_id, id DESC"),
                @Index(name = "idx_reviews_customer_id", columnList = "customer_id, ID DESC"),
        }
)
public class Review extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Order order;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
