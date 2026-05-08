package com.inkwell.comment_like_service.controller;

import com.inkwell.comment_like_service.dto.CommentRequest;
import com.inkwell.comment_like_service.dto.UpdateCommentRequest;
import com.inkwell.comment_like_service.model.Comment;
import com.inkwell.comment_like_service.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping({"", "/"})
    public ResponseEntity<Comment> add(@RequestHeader(value = "X-User-Role", required = false) String role,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                       @Valid @RequestBody CommentRequest request) {
        Long effectiveUserId = resolveUserId(userId, request.getAuthorId());
        request.setAuthorId(effectiveUserId);
        return ResponseEntity.ok(commentService.addComment(request));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> byPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getByPost(postId));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<Comment> byId(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getComment(commentId));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Comment> update(@PathVariable Long commentId,
                                          @RequestHeader(value = "X-User-Role", required = false) String role,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                          @RequestParam(value = "userId", required = false) Long userIdParam,
                                          @Valid @RequestBody UpdateCommentRequest request) {
        Long effectiveUserId = resolveUserId(userIdHeader, userIdParam);
        return ResponseEntity.ok(commentService.updateComment(commentId, effectiveUserId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId,
                                       @RequestHeader(value = "X-User-Role", required = false) String role,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                       @RequestParam(value = "userId", required = false) Long userIdParam) {
        Long effectiveUserId = resolveUserId(userIdHeader, userIdParam);
        commentService.deleteComment(commentId, effectiveUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{commentId}/approve")
    public ResponseEntity<Comment> approve(@RequestHeader(value = "X-User-Role", required = false) String role,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @PathVariable Long commentId) {
        requireAnyRole(role, "AUTHOR", "ADMIN");
        return ResponseEntity.ok(commentService.approve(commentId, userId, role));
    }

    @PutMapping("/{commentId}/reject")
    public ResponseEntity<Comment> reject(@RequestHeader(value = "X-User-Role", required = false) String role,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                          @PathVariable Long commentId) {
        requireAnyRole(role, "AUTHOR", "ADMIN");
        return ResponseEntity.ok(commentService.reject(commentId, userId, role));
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<Void> deleteByPost(@PathVariable Long postId) {
        commentService.deleteByPost(postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Comment> like(@RequestHeader(value = "X-User-Role", required = false) String role,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                        @PathVariable Long commentId,
                                        @RequestParam(value = "userId", required = false) Long userIdParam) {
        Long effectiveUserId = resolveUserId(userIdHeader, userIdParam);
        return ResponseEntity.ok(commentService.like(commentId, effectiveUserId));
    }

    @PostMapping("/{commentId}/unlike")
    public ResponseEntity<Comment> unlike(@RequestHeader(value = "X-User-Role", required = false) String role,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
                                          @PathVariable Long commentId,
                                          @RequestParam(value = "userId", required = false) Long userIdParam) {
        Long effectiveUserId = resolveUserId(userIdHeader, userIdParam);
        return ResponseEntity.ok(commentService.unlike(commentId, effectiveUserId));
    }

    @GetMapping("/count/{postId}")
    public ResponseEntity<Map<String, Long>> count(@PathVariable Long postId) {
        return ResponseEntity.ok(Map.of("count", commentService.countByPost(postId)));
    }

    @GetMapping("/count-total")
    public ResponseEntity<Map<String, Long>> countTotal() {
        return ResponseEntity.ok(Map.of("count", commentService.countAll()));
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

    private Long resolveUserId(Long headerUserId, Long fallbackUserId) {
        Long effectiveUserId = headerUserId != null ? headerUserId : fallbackUserId;
        if (effectiveUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
        return effectiveUserId;
    }
}
