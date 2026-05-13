package com.inkwell.post_category_tag_service.repository;

import com.inkwell.post_category_tag_service.model.Post;
import com.inkwell.post_category_tag_service.model.PostStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);

    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> findByStatusOrderByPublishedAtDesc(PostStatus status);

    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> findByAuthorIdOrderByIdDesc(Long authorId);

    @Query("SELECT p FROM Post p WHERE p.status = :status AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(p.excerpt, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.publishedAt DESC")
    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> searchPublishedByKeyword(@Param("status") PostStatus status, @Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> findByStatusAndCategories_IdOrderByPublishedAtDesc(PostStatus status, Long categoryId);

    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> findByStatusAndTags_IdOrderByPublishedAtDesc(PostStatus status, Long tagId);
    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> findAllByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Post> findAllByOrderByFeaturedDescPublishedAtDesc();

    List<Post> findTop5ByStatusOrderByViewCountDesc(PostStatus status);

    @Query("SELECT p.authorId, COUNT(p) FROM Post p GROUP BY p.authorId ORDER BY COUNT(p) DESC")
    List<Object[]> findMostActiveAuthors();
}
