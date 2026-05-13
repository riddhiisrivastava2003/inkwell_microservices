package com.inkwell.post_category_tag_service.repository;

import com.inkwell.post_category_tag_service.model.PostViewSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewSessionRepository extends JpaRepository<PostViewSession, Long> {
    boolean existsByPostIdAndSessionKey(Long postId, String sessionKey);

    void deleteByPostId(Long postId);
}
