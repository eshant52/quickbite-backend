package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Review extends Base {
    @ManyToOne
    private Restaurant restaurant;

    @ManyToOne
    private User customer;

    @ManyToOne
    private Order order;

    private int rating;
    private String comment;
}
