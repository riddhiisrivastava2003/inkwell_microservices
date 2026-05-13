package com.inkwell.post_category_tag_service.service;

import com.inkwell.post_category_tag_service.dto.CategoryRequest;
import com.inkwell.post_category_tag_service.dto.PostRequest;
import com.inkwell.post_category_tag_service.dto.PostSummaryResponse;
import com.inkwell.post_category_tag_service.dto.TagRequest;
import com.inkwell.post_category_tag_service.config.AppConfig;
import com.inkwell.post_category_tag_service.exception.BadRequestException;
import com.inkwell.post_category_tag_service.model.*;
import com.inkwell.post_category_tag_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostViewSessionRepository postViewSessionRepository;
    private final SavedPostRepository savedPostRepository;
    private final WebClient webClient;
    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Value("${services.newsletter.base-url}")
    private String newsletterBaseUrl;
    @Value("${services.comment.base-url}")
    private String commentBaseUrl;
    @Value("${services.auth.base-url}")
    private String authBaseUrl;
    @Value("${services.notification.base-url}")
    private String notificationBaseUrl;

    @CacheEvict(value = "publishedPosts", allEntries = true)
    public Post createPost(PostRequest request) {
        Post post = new Post();
        copyPostFields(post, request, true);
        Post saved = postRepository.save(post);
        if (saved.getStatus() == PostStatus.PUBLISHED) {
            if (saved.getPublishedAt() == null) {
                saved.setPublishedAt(LocalDateTime.now());
                saved = postRepository.save(saved);
            }
            notifyNewsletter(saved);
            notifyFollowers(saved);
        }
        return saved;
    }

    @CacheEvict(value = "publishedPosts", allEntries = true)
    public Post updatePost(Long postId, PostRequest request) {
        Post post = getPostById(postId);
        PostStatus previousStatus = post.getStatus();
        copyPostFields(post, request, false);
        Post saved = postRepository.save(post);
        if (saved.getStatus() == PostStatus.PUBLISHED && previousStatus != PostStatus.PUBLISHED) {
            if (saved.getPublishedAt() == null) {
                saved.setPublishedAt(LocalDateTime.now());
                saved = postRepository.save(saved);
            }
            notifyNewsletter(saved);
            notifyFollowers(saved);
        }
        return saved;
    }

    public List<Post> getPublishedPosts(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchPublishedByKeyword(PostStatus.PUBLISHED, keyword);
        }
        return postRepository.findAllByOrderByFeaturedDescPublishedAtDesc()
                .stream()
                .filter(post -> post.getStatus() == PostStatus.PUBLISHED)
                .toList();
    }

    public List<Post> getPublishedByCategory(Long categoryId) {
        return postRepository.findByStatusAndCategories_IdOrderByPublishedAtDesc(PostStatus.PUBLISHED, categoryId);
    }

    public List<Post> getPublishedByTag(Long tagId) {
        return postRepository.findByStatusAndTags_IdOrderByPublishedAtDesc(PostStatus.PUBLISHED, tagId);
    }

    public List<Post> getPostsByAuthor(Long authorId) {
        return postRepository.findByAuthorIdOrderByIdDesc(authorId);
    }

    public List<Post> getAllPostsForAdmin() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public Post getPostBySlug(String slug) {
        return postRepository.findBySlug(slug).orElseThrow(() -> new BadRequestException("Post not found"));
    }

    public Post getPostById(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new BadRequestException("Post not found"));
    }

    public PostSummaryResponse getSummary(Long postId) {
        Post post = getPostById(postId);
        return PostSummaryResponse.builder()
                .postId(post.getId())
                .authorId(post.getAuthorId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .status(post.getStatus().name())
                .build();
    }

    @Transactional
    @CacheEvict(value = "publishedPosts", allEntries = true)
    public Post changeStatus(Long postId, PostStatus status) {
        Post post = getPostById(postId);
        PostStatus previousStatus = post.getStatus();
        post.setStatus(status);

        if (status == PostStatus.PUBLISHED && previousStatus != PostStatus.PUBLISHED) {
            if (post.getPublishedAt() == null) {
                post.setPublishedAt(LocalDateTime.now());
            }
            notifyNewsletter(post);
            notifyFollowers(post);
        }
        return postRepository.save(post);
    }

    @Transactional
    @CacheEvict(value = "publishedPosts", allEntries = true)
    public void deletePost(Long postId, Long actorUserId) {
        getPostById(postId);
        try {
            webClient.delete().uri(commentBaseUrl + "/api/comments/post/" + postId).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // Post deletion should proceed even if comment cleanup fails.
        }

        forceCleanupPostReferences(postId);
        jdbcTemplate.update("DELETE FROM posts WHERE post_id = ?", postId);
        logAudit(actorUserId, "DELETE_POST", "POST", postId, "post deleted");
    }

    @CacheEvict(value = "publishedPosts", allEntries = true)
    public Post featurePost(Long postId, boolean featured, Long actorUserId) {
        Post post = getPostById(postId);
        post.setFeatured(featured);
        Post saved = postRepository.save(post);
        logAudit(actorUserId, featured ? "FEATURE_POST" : "UNFEATURE_POST", "POST", postId, "featured=" + featured);
        return saved;
    }

    public Post likePost(Long postId, Long userId, boolean like) {
        if (userId == null && !like) {
            // If identity propagation fails for an unlike call, treat as no-op.
            return getPostById(postId);
        }
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }

        Post post = getPostById(postId);
        boolean likedAlready = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        if (like && !likedAlready) {
            PostLike postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setUserId(userId);
            postLikeRepository.save(postLike);
            post.setLikesCount(post.getLikesCount() + 1);
            return postRepository.save(post);
        }

        if (!like && likedAlready) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            return postRepository.save(post);
        }

        return post;
    }

    public Post incrementView(Long postId, String sessionKey) {
        Post post = getPostById(postId);
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new BadRequestException("sessionKey is required");
        }

        boolean exists = postViewSessionRepository.existsByPostIdAndSessionKey(postId, sessionKey);
        if (!exists) {
            PostViewSession viewSession = new PostViewSession();
            viewSession.setPostId(postId);
            viewSession.setSessionKey(sessionKey);
            postViewSessionRepository.save(viewSession);

            post.setViewCount(post.getViewCount() + 1);
            post = postRepository.save(post);
        }
        return post;
    }

    public Map<String, Object> savePost(Long postId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        getPostById(postId);

        if (!savedPostRepository.existsByUserIdAndPostId(userId, postId)) {
            SavedPost savedPost = new SavedPost();
            savedPost.setUserId(userId);
            savedPost.setPostId(postId);
            savedPostRepository.save(savedPost);
        }
        return Map.of("saved", true);
    }

    public Map<String, Object> unsavePost(Long postId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        savedPostRepository.deleteByUserIdAndPostId(userId, postId);
        return Map.of("saved", false);
    }

    public Map<String, Object> saveStatus(Long postId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        return Map.of("saved", savedPostRepository.existsByUserIdAndPostId(userId, postId));
    }

    public List<Post> savedPosts(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        List<Long> postIds = savedPostRepository.findByUserIdOrderBySavedAtDesc(userId)
                .stream()
                .map(SavedPost::getPostId)
                .toList();
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Post> postMap = postRepository.findAllById(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(Post::getId, p -> p));
        return postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public Category createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(uniqueSlug(request.getName(), true));
        category.setDescription(request.getDescription());
        category.setParentCategoryId(request.getParentCategoryId());
        return categoryRepository.save(category);
    }

    public List<Category> allCategories() {
        return categoryRepository.findAll();
    }

    public Category updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new BadRequestException("Category not found"));
        category.setName(request.getName());
        category.setSlug(uniqueSlug(request.getName(), true));
        category.setDescription(request.getDescription());
        category.setParentCategoryId(request.getParentCategoryId());
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    public Tag createTag(TagRequest request) {
        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setSlug(uniqueSlug(request.getName(), false));
        return tagRepository.save(tag);
    }

    public List<Tag> allTags() {
        return tagRepository.findAll();
    }

    public List<Tag> trendingTags() {
        return tagRepository.findTop10ByOrderByPostCountDesc();
    }

    public void deleteTag(Long tagId) {
        tagRepository.deleteById(tagId);
    }

    public Map<String, Object> analytics() {
        long totalPosts = postRepository.count();
        long totalCategories = categoryRepository.count();
        long totalTags = tagRepository.count();

        List<Map<String, Object>> topPosts = postRepository.findTop5ByStatusOrderByViewCountDesc(PostStatus.PUBLISHED)
                .stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("postId", p.getId());
                    item.put("title", p.getTitle());
                    item.put("slug", p.getSlug());
                    item.put("viewCount", p.getViewCount());
                    item.put("likesCount", p.getLikesCount());
                    return item;
                })
                .toList();

        List<Map<String, Object>> activeAuthors = postRepository.findMostActiveAuthors()
                .stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("authorId", row[0]);
                    item.put("postCount", row[1]);
                    return item;
                })
                .toList();

        return Map.of(
                "totalPosts", totalPosts,
                "totalCategories", totalCategories,
                "totalTags", totalTags,
                "mostViewedPosts", topPosts,
                "mostActiveAuthors", activeAuthors
        );
    }

    private void copyPostFields(Post post, PostRequest request, boolean newPost) {
        post.setAuthorId(request.getAuthorId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setExcerpt(request.getExcerpt());
        String featuredImageUrl = request.getFeaturedImageUrl();
        post.setFeaturedImageUrl(featuredImageUrl == null || featuredImageUrl.isBlank() ? null : featuredImageUrl);
        post.setStatus(request.getStatus());

        if (newPost || post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(generateUniquePostSlug(request.getTitle(), post.getId()));
        }

        post.setReadTimeMin(Math.max(1, calculateReadTime(request.getContent())));

        Set<Category> categories = new HashSet<>();
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            categories.addAll(categoryRepository.findAllById(request.getCategoryIds()));
        }

        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags.addAll(tagRepository.findAllById(request.getTagIds()));
        }

        post.setCategories(categories);
        post.setTags(tags);

        updateCounts(categories, tags);
    }

    private void updateCounts(Set<Category> categories, Set<Tag> tags) {
        for (Category category : categories) {
            category.setPostCount(Math.max(0, category.getPostCount() + 1));
        }
        categoryRepository.saveAll(categories);

        for (Tag tag : tags) {
            tag.setPostCount(Math.max(0, tag.getPostCount() + 1));
        }
        tagRepository.saveAll(tags);
    }

    private int calculateReadTime(String content) {
        String plain = content == null ? "" : content.replaceAll("<[^>]*>", " ");
        int words = plain.trim().isEmpty() ? 0 : plain.trim().split("\\s+").length;
        return (int) Math.ceil(words / 200.0);
    }

    private String generateUniquePostSlug(String title, Long currentPostId) {
        String base = toSlug(title);
        String candidate = base;
        int suffix = 1;

        while (true) {
            Optional<Post> existing = postRepository.findBySlug(candidate);
            if (existing.isEmpty() || (currentPostId != null && existing.get().getId().equals(currentPostId))) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
        }
    }

    private String uniqueSlug(String name, boolean category) {
        String base = toSlug(name);
        String candidate = base;
        int suffix = 1;

        while (true) {
            boolean exists = category
                    ? categoryRepository.findBySlug(candidate).isPresent()
                    : tagRepository.findBySlug(candidate).isPresent();

            if (!exists) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
        }
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "post" : slug;
    }

    private void notifyNewsletter(Post post) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("postId", post.getId());
            payload.put("title", post.getTitle());
            payload.put("slug", post.getSlug());
            payload.put("authorId", post.getAuthorId());
            rabbitTemplate.convertAndSend(AppConfig.POST_EVENTS_EXCHANGE, AppConfig.NEW_POST_ROUTING_KEY, payload);
            webClient.post().uri(newsletterBaseUrl + "/internal/newsletter/new-post").bodyValue(payload).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // Post publish should not fail if newsletter service is down.
        }
    }

    private void notifyFollowers(Post post) {
        try {
            Map followersResponse = webClient.get().uri(
                    authBaseUrl + "/api/auth/internal/users/" + post.getAuthorId() + "/followers").retrieve().bodyToMono(Map.class
            ).block();
            if (followersResponse == null || followersResponse.get("followerIds") == null) {
                return;
            }

            List<Long> followerIds = new ArrayList<>();
            List rawIds = (List) followersResponse.get("followerIds");
            for (Object id : rawIds) {
                followerIds.add(Long.valueOf(String.valueOf(id)));
            }

            if (followerIds.isEmpty()) {
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientIds", followerIds);
            payload.put("actorId", post.getAuthorId());
            payload.put("type", "NEW_POST");
            payload.put("title", "New post from an author you follow");
            payload.put("message", post.getTitle());
            payload.put("relatedId", post.getId());
            payload.put("relatedType", "POST");
            webClient.post().uri(notificationBaseUrl + "/api/notifications/internal/bulk").bodyValue(payload).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // Follower notifications are best-effort.
        }
    }

    private void logAudit(Long actorUserId, String action, String entityType, Long entityId, String details) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("actorUserId", actorUserId);
            payload.put("action", action);
            payload.put("entityType", entityType);
            payload.put("entityId", String.valueOf(entityId));
            payload.put("details", details);
            webClient.post().uri(authBaseUrl + "/api/auth/internal/audit-log").bodyValue(payload).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // Audits are best-effort.
        }
    }

    private void forceCleanupPostReferences(Long postId) {
        // Defensive cleanup for legacy schemas/FK constraints.
        List<String> cleanupSql = List.of(
                "DELETE FROM post_categories WHERE post_id = ?",
                "DELETE FROM post_tags WHERE post_id = ?",
                "DELETE FROM post_likes WHERE post_id = ?",
                "DELETE FROM post_view_sessions WHERE post_id = ?",
                "DELETE FROM saved_posts WHERE post_id = ?"
        );
        for (String sql : cleanupSql) {
            try {
                jdbcTemplate.update(sql, postId);
            } catch (Exception ignored) {
                // Best-effort cleanup; ignore non-existing table/column variations.
            }
        }
    }
}


