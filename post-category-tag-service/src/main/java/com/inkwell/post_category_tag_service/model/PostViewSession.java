package com.inkwell.post_category_tag_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_view_sessions", uniqueConstraints = @UniqueConstraint(name = "uk_post_session", columnNames = {"postId", "sessionKey"}))
@Getter
@Setter
public class PostViewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_session_id")
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false, length = 120)
    private String sessionKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
