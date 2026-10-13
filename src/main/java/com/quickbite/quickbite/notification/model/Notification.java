package com.quickbite.quickbite.notification.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_unread_desc", columnList = "recipient_id, is_read, id DESC"),
                @Index(name = "idx_notifications_recipient_desc", columnList = "recipient_id, id DESC")
        }
)
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
