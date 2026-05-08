package com.inkwell.notification_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipientId;
    private Long actorId;
    private String type;
    private String title;

    @Column(length = 1000)
    private String message;

    private Long relatedId;
    private String relatedType;
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
