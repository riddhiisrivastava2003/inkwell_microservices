package com.inkwell.media_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Getter
@Setter
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long uploaderId;
    private String filename;
    private String originalName;
    private String url;
    private String mimeType;
    private Long sizeKb;
    private String altText;
    private Long linkedPostId;
    private boolean deleted = false;
    private LocalDateTime uploadedAt;

    @PrePersist
    public void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
