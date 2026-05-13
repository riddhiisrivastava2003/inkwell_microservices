package com.inkwell.post_category_tag_service.repository;

import com.inkwell.post_category_tag_service.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);

    List<Tag> findTop10ByOrderByPostCountDesc();
}
