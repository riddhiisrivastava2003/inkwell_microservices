package com.inkwell.comment_like_service.repository;

import com.inkwell.comment_like_service.model.Comment;
import com.inkwell.comment_like_service.model.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<Comment> findByParentCommentId(Long parentCommentId);

    long countByPostId(Long postId);

    List<Comment> findByStatus(CommentStatus status);
}
