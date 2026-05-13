package com.inkwell.post_category_tag_service.repository;

import com.inkwell.post_category_tag_service.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostId(Long postId);
}
