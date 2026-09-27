package com.quickbite.quickbite.notification.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notifications")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Notification extends Base {

    @Column(length = 200, nullable = false)
    private String title;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User recipient;

    @Column(nullable = false)
    private boolean isRead;
}
