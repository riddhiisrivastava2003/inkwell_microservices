package com.inkwell.media_service.repository;

import com.inkwell.media_service.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUploaderIdAndDeletedFalse(Long uploaderId);

    List<Media> findByLinkedPostIdAndDeletedFalse(Long linkedPostId);

    List<Media> findByDeletedFalse();
}
