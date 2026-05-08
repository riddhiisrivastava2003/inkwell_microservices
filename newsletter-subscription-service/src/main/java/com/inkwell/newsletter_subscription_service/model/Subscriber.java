package com.inkwell.newsletter_subscription_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscribers", uniqueConstraints = @UniqueConstraint(name = "uk_subscriber_email", columnNames = "email"))
@Getter
@Setter
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private Long userId;
    private String fullName;

    @Enumerated(EnumType.STRING)
    private SubscriberStatus status = SubscriberStatus.PENDING;

    @Column(length = 80)
    private String token;

    private LocalDateTime tokenExpiresAt;

    @Column(length = 500)
    private String preferences;

    private LocalDateTime subscribedAt;
    private LocalDateTime unsubscribedAt;

    @PrePersist
    public void onCreate() {
        subscribedAt = LocalDateTime.now();
    }
}
