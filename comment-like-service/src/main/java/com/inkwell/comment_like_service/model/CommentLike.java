package com.inkwell.comment_like_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comment_likes", uniqueConstraints = @UniqueConstraint(name = "uk_comment_user", columnNames = {"commentId", "userId"}))
@Getter
@Setter
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long commentId;
    private Long userId;
}
