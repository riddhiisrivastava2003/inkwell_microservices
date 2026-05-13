package com.inkwell.post_category_tag_service.controller;

import com.inkwell.post_category_tag_service.dto.CategoryRequest;
import com.inkwell.post_category_tag_service.dto.PostRequest;
import com.inkwell.post_category_tag_service.dto.PostSummaryResponse;
import com.inkwell.post_category_tag_service.dto.TagRequest;
import com.inkwell.post_category_tag_service.model.Category;
import com.inkwell.post_category_tag_service.model.Post;
import com.inkwell.post_category_tag_service.model.PostStatus;
import com.inkwell.post_category_tag_service.model.Tag;
import com.inkwell.post_category_tag_service.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @Valid @RequestBody PostRequest request) {
        requireAnyRole(userRole, "AUTHOR", "ADMIN");
        request.setAuthorId(userId != null ? userId : request.getAuthorId());
        return ResponseEntity.ok(postService.createPost(request));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<Post> updatePost(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @PathVariable Long postId,
                                           @Valid @RequestBody PostRequest request) {
        requireAnyRole(userRole, "AUTHOR", "ADMIN");
        request.setAuthorId(userId != null ? userId : request.getAuthorId());
        return ResponseEntity.ok(postService.updatePost(postId, request));
    }

    @PutMapping("/posts/{postId}/status")
    public ResponseEntity<Post> changeStatus(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                             @PathVariable Long postId,
                                             @RequestParam PostStatus status) {
        requireAnyRole(userRole, "AUTHOR", "ADMIN");
        return ResponseEntity.ok(postService.changeStatus(postId, status));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @PathVariable Long postId) {
        requireAnyRole(userRole, "AUTHOR", "ADMIN");
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/published")
    public ResponseEntity<List<Post>> published(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(postService.getPublishedPosts(keyword));
    }

    @GetMapping("/posts/category/{categoryId}")
    public ResponseEntity<List<Post>> publishedByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(postService.getPublishedByCategory(categoryId));
    }

    @GetMapping("/posts/tag/{tagId}")
    public ResponseEntity<List<Post>> publishedByTag(@PathVariable Long tagId) {
        return ResponseEntity.ok(postService.getPublishedByTag(tagId));
    }

    @GetMapping("/posts/author/{authorId}")
    public ResponseEntity<List<Post>> byAuthor(@PathVariable Long authorId) {
        return ResponseEntity.ok(postService.getPostsByAuthor(authorId));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<Post> byId(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @GetMapping("/posts/slug/{slug}")
    public ResponseEntity<Post> bySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPostBySlug(slug));
    }

    @GetMapping("/posts/{postId}/summary")
    public ResponseEntity<PostSummaryResponse> summary(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getSummary(postId));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Post> like(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                     @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                     @PathVariable Long postId,
                                     @RequestParam(required = false) Long userId,
                                     @RequestParam(defaultValue = "true") boolean like) {
        requireAnyRole(userRole, "READER", "AUTHOR", "ADMIN");
        Long resolvedUserId = userIdHeader != null ? userIdHeader : userId;
        return ResponseEntity.ok(postService.likePost(postId, resolvedUserId, like));
    }

    @PostMapping("/posts/{postId}/view")
    public ResponseEntity<Post> incrementView(@PathVariable Long postId, @RequestParam String sessionKey) {
        return ResponseEntity.ok(postService.incrementView(postId, sessionKey));
    }

    @PostMapping("/posts/{postId}/save")
    public ResponseEntity<?> savePost(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                      @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                      @PathVariable Long postId) {
        requireAnyRole(userRole, "READER", "AUTHOR", "ADMIN");
        return ResponseEntity.ok(postService.savePost(postId, userId));
    }

    @DeleteMapping("/posts/{postId}/save")
    public ResponseEntity<?> unsavePost(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                        @PathVariable Long postId) {
        requireAnyRole(userRole, "READER", "AUTHOR", "ADMIN");
        return ResponseEntity.ok(postService.unsavePost(postId, userId));
    }

    @GetMapping("/posts/{postId}/save-status")
    public ResponseEntity<?> saveStatus(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                        @PathVariable Long postId) {
        requireAnyRole(userRole, "READER", "AUTHOR", "ADMIN");
        return ResponseEntity.ok(postService.saveStatus(postId, userId));
    }

    @GetMapping("/posts/saved")
    public ResponseEntity<List<Post>> savedPosts(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireAnyRole(userRole, "READER", "AUTHOR", "ADMIN");
        return ResponseEntity.ok(postService.savedPosts(userId));
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                   @Valid @RequestBody CategoryRequest request) {
        requireAnyRole(userRole, "ADMIN");
        return ResponseEntity.ok(postService.createCategory(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(postService.allCategories());
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<Category> updateCategory(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                                   @PathVariable Long categoryId,
                                                   @Valid @RequestBody CategoryRequest request) {
        requireAnyRole(userRole, "ADMIN");
        return ResponseEntity.ok(postService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                               @PathVariable Long categoryId) {
        requireAnyRole(userRole, "ADMIN");
        postService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tags")
    public ResponseEntity<Tag> createTag(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                         @Valid @RequestBody TagRequest request) {
        requireAnyRole(userRole, "ADMIN");
        return ResponseEntity.ok(postService.createTag(request));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> tags() {
        return ResponseEntity.ok(postService.allTags());
    }

    @GetMapping("/tags/trending")
    public ResponseEntity<List<Tag>> trendingTags() {
        return ResponseEntity.ok(postService.trendingTags());
    }

    @DeleteMapping("/tags/{tagId}")
    public ResponseEntity<Void> deleteTag(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                          @PathVariable Long tagId) {
        requireAnyRole(userRole, "ADMIN");
        postService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/analytics")
    public ResponseEntity<?> analytics(@RequestHeader(value = "X-User-Role", required = false) String userRole) {
        requireAnyRole(userRole, "ADMIN");
        return ResponseEntity.ok(postService.analytics());
    }

    @GetMapping("/admin/posts")
    public ResponseEntity<List<Post>> adminPosts(@RequestHeader(value = "X-User-Role", required = false) String userRole) {
        requireAnyRole(userRole, "ADMIN");
        return ResponseEntity.ok(postService.getAllPostsForAdmin());
    }

    @PutMapping("/admin/posts/{postId}/feature")
    public ResponseEntity<Post> featurePost(@RequestHeader(value = "X-User-Role", required = false) String userRole,
                                            @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                            @PathVariable Long postId,
                                            @RequestParam boolean featured) {
        requireAnyRole(userRole, "ADMIN");
        return ResponseEntity.ok(postService.featurePost(postId, featured, userId));
    }

    private void requireAnyRole(String role, String... allowed) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing user role");
        }
        for (String allow : allowed) {
            if (allow.equalsIgnoreCase(role)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
    }
}
