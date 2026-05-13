package com.inkwell.comment_like_service.service;

import com.inkwell.comment_like_service.dto.CommentRequest;
import com.inkwell.comment_like_service.dto.UpdateCommentRequest;
import com.inkwell.comment_like_service.exception.BadRequestException;
import com.inkwell.comment_like_service.model.Comment;
import com.inkwell.comment_like_service.model.CommentLike;
import com.inkwell.comment_like_service.model.CommentStatus;
import com.inkwell.comment_like_service.repository.CommentLikeRepository;
import com.inkwell.comment_like_service.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final WebClient webClient;

    @Value("${comments.edit-window-minutes}")
    private long editWindowMinutes;

    @Value("${services.post.base-url}")
    private String postBaseUrl;

    @Value("${services.notification.base-url}")
    private String notificationBaseUrl;

    @Value("${services.auth.base-url}")
    private String authBaseUrl;

    @Value("${comments.moderation-required:false}")
    private boolean moderationRequired;

    public Comment addComment(CommentRequest request) {
        if (request.getParentCommentId() != null) {
            Comment parent = getComment(request.getParentCommentId());
            if (parent.getParentCommentId() != null) {
                throw new BadRequestException("Only two-level threading allowed");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setAuthorId(request.getAuthorId());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setContent(request.getContent());
        comment.setStatus(moderationRequired ? CommentStatus.PENDING : CommentStatus.APPROVED);
        Comment saved = commentRepository.save(comment);

        sendNotifications(saved);
        return saved;
    }

    public List<Comment> getByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public Comment getComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new BadRequestException("Comment not found"));
    }

    public Comment updateComment(Long commentId, Long userId, UpdateCommentRequest request) {
        Comment comment = getComment(commentId);
        if (!Objects.equals(comment.getAuthorId(), userId)) {
            throw new BadRequestException("You can edit only your comment");
        }

        if (comment.getCreatedAt().plusMinutes(editWindowMinutes).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Edit window expired");
        }

        comment.setContent(request.getContent());
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = getComment(commentId);
        if (!Objects.equals(comment.getAuthorId(), userId)) {
            throw new BadRequestException("You can delete only your comment");
        }

        softDelete(comment);
        List<Comment> replies = commentRepository.findByParentCommentId(commentId);
        for (Comment reply : replies) {
            softDelete(reply);
        }
    }

    public Comment approve(Long commentId, Long actorUserId, String actorRole) {
        ensureCanModerate(commentId, actorUserId, actorRole);
        Comment comment = getComment(commentId);
        comment.setStatus(CommentStatus.APPROVED);
        Comment saved = commentRepository.save(comment);
        logAudit(actorUserId, "APPROVE_COMMENT", "COMMENT", commentId, "status=APPROVED");
        return saved;
    }

    public Comment reject(Long commentId, Long actorUserId, String actorRole) {
        ensureCanModerate(commentId, actorUserId, actorRole);
        Comment comment = getComment(commentId);
        comment.setStatus(CommentStatus.REJECTED);
        Comment saved = commentRepository.save(comment);
        logAudit(actorUserId, "REJECT_COMMENT", "COMMENT", commentId, "status=REJECTED");
        return saved;
    }

    public void deleteByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        comments.forEach(this::softDelete);
    }

    public Comment like(Long commentId, Long userId) {
        Comment comment = getComment(commentId);
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeRepository.save(like);
            comment.setLikesCount(comment.getLikesCount() + 1);
            comment = commentRepository.save(comment);
        }
        return comment;
    }

    public Comment unlike(Long commentId, Long userId) {
        Comment comment = getComment(commentId);
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
            comment = commentRepository.save(comment);
        }
        return comment;
    }

    public long countByPost(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public long countAll() {
        return commentRepository.count();
    }

    private void softDelete(Comment comment) {
        comment.setStatus(CommentStatus.DELETED);
        comment.setContent("[deleted]");
        commentRepository.save(comment);
    }

    private void sendNotifications(Comment comment) {
        try {
            Set<Long> recipients = new HashSet<>();
            Map postSummary = webClient.get().uri(postBaseUrl + "/api/posts/" + comment.getPostId() + "/summary").retrieve().bodyToMono(Map.class).block();
            if (postSummary == null) {
                return;
            }

            Long authorId = Long.valueOf(String.valueOf(postSummary.get("authorId")));
            if (!authorId.equals(comment.getAuthorId())) {
                recipients.add(authorId);
                Map<String, Object> payload = new HashMap<>();
                payload.put("recipientId", authorId);
                payload.put("actorId", comment.getAuthorId());
                payload.put("type", "NEW_COMMENT");
                payload.put("title", "New comment on your post");
                payload.put("message", "A user commented on post: " + postSummary.get("title"));
                payload.put("relatedId", comment.getPostId());
                payload.put("relatedType", "POST");
                webClient.post().uri(notificationBaseUrl + "/api/notifications").bodyValue(payload).retrieve().toBodilessEntity().block();
            }

            if (comment.getParentCommentId() != null) {
                Comment parent = getComment(comment.getParentCommentId());
                if (!Objects.equals(parent.getAuthorId(), comment.getAuthorId())) {
                    recipients.add(parent.getAuthorId());
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("recipientId", parent.getAuthorId());
                    payload.put("actorId", comment.getAuthorId());
                    payload.put("type", "COMMENT_REPLY");
                    payload.put("title", "New reply to your comment");
                    payload.put("message", comment.getContent());
                    payload.put("relatedId", comment.getPostId());
                    payload.put("relatedType", "POST");
                    webClient.post().uri(notificationBaseUrl + "/api/notifications").bodyValue(payload).retrieve().toBodilessEntity().block();
                }
            }

            for (Long mentionUserId : resolveMentionUserIds(comment.getContent())) {
                if (mentionUserId.equals(comment.getAuthorId()) || recipients.contains(mentionUserId)) {
                    continue;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("recipientId", mentionUserId);
                payload.put("actorId", comment.getAuthorId());
                payload.put("type", "MENTION");
                payload.put("title", "You were mentioned in a comment");
                payload.put("message", comment.getContent());
                payload.put("relatedId", comment.getPostId());
                payload.put("relatedType", "POST");
                webClient.post().uri(notificationBaseUrl + "/api/notifications").bodyValue(payload).retrieve().toBodilessEntity().block();
            }
        } catch (Exception ignored) {
            // Notification failures should not block comments.
        }
    }

    private Set<Long> resolveMentionUserIds(String content) {
        Set<Long> userIds = new HashSet<>();
        if (content == null || content.isBlank()) {
            return userIds;
        }
        Pattern mentionPattern = Pattern.compile("@([A-Za-z0-9_]{3,50})");
        Matcher matcher = mentionPattern.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            try {
                Map[] users = webClient.get().uri(authBaseUrl + "/api/auth/search?username=" + username).retrieve().bodyToMono(Map[].class).block();
                if (users != null) {
                    for (Map user : users) {
                        if (username.equalsIgnoreCase(String.valueOf(user.get("username")))) {
                            userIds.add(Long.valueOf(String.valueOf(user.get("id"))));
                        }
                    }
                }
            } catch (Exception ignored) {
                // Mentions are best-effort.
            }
        }
        return userIds;
    }

    private void ensureCanModerate(Long commentId, Long actorUserId, String actorRole) {
        if ("ADMIN".equalsIgnoreCase(actorRole)) {
            return;
        }
        Comment comment = getComment(commentId);
        Map postSummary = webClient.get().uri(postBaseUrl + "/api/posts/" + comment.getPostId() + "/summary").retrieve().bodyToMono(Map.class).block();
        if (postSummary == null || actorUserId == null) {
            throw new BadRequestException("Not allowed to moderate this comment");
        }
        Long authorId = Long.valueOf(String.valueOf(postSummary.get("authorId")));
        if (!authorId.equals(actorUserId)) {
            throw new BadRequestException("Authors can moderate comments only on their own posts");
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
            // best effort
        }
    }
}


