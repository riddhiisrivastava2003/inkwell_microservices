package com.inkwell.post_category_tag_service.repository;

import com.inkwell.post_category_tag_service.model.SavedPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    List<SavedPost> findByUserIdOrderBySavedAtDesc(Long userId);

    void deleteByPostId(Long postId);
}

