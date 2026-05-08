package com.inkwell.auth_service.repository;

import com.inkwell.auth_service.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    List<UserFollow> findByFolloweeId(Long followeeId);
    List<UserFollow> findByFollowerId(Long followerId);

    long countByFolloweeId(Long followeeId);

    void deleteByFollowerIdOrFolloweeId(Long followerId, Long followeeId);
}
