package com.inkwell.auth_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_follows", uniqueConstraints = {
        @UniqueConstraint(name = "uk_follower_followee", columnNames = {"followerId", "followeeId"})
})
@Getter
@Setter
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long followerId;

    @Column(nullable = false)
    private Long followeeId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

