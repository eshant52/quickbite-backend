package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Notification extends Base {
    private String title;
    private String message;

    @ManyToOne
    private User recipient;

    private boolean isRead;
}
