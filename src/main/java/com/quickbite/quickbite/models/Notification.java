package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notifications")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Notification extends Base {
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    @Column(length = 200, nullable = false)
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must be at most 1000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User recipient;

    @Column(nullable = false)
    private boolean isRead;
}
