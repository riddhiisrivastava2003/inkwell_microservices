package com.inkwell.notification_service.service;

import com.inkwell.notification_service.exception.BadRequestException;
import com.inkwell.notification_service.model.Notification;
import com.inkwell.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebClient webClient;

    @Value("${services.auth.base-url}")
    private String authBaseUrl;

    public Notification send(com.inkwell.notification_service.dto.NotificationRequest request) {
        if (request.getRecipientId() == null) {
            throw new BadRequestException("recipientId is required");
        }
        Notification notification = new Notification();
        notification.setRecipientId(request.getRecipientId());
        notification.setActorId(request.getActorId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRelatedId(request.getRelatedId());
        notification.setRelatedType(request.getRelatedType());
        return notificationRepository.save(notification);
    }

    public List<Notification> sendBulk(com.inkwell.notification_service.dto.BulkNotificationRequest request) {
        if (request.getRecipientIds() == null || request.getRecipientIds().isEmpty()) {
            throw new BadRequestException("recipientIds are required");
        }

        List<Notification> notifications = request.getRecipientIds().stream().map(recipientId -> {
            Notification n = new Notification();
            n.setRecipientId(recipientId);
            n.setActorId(request.getActorId());
            n.setType(request.getType());
            n.setTitle(request.getTitle());
            n.setMessage(request.getMessage());
            n.setRelatedId(request.getRelatedId());
            n.setRelatedType(request.getRelatedType());
            return n;
        }).toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);
        logAudit(request.getActorId(), "SEND_BULK_NOTIFICATION", "NOTIFICATION", "bulk",
                "recipients=" + request.getRecipientIds().size());
        return saved;
    }

    public List<Notification> sendByRole(com.inkwell.notification_service.dto.RoleBroadcastRequest request) {
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new BadRequestException("role is required");
        }
        Map[] users = webClient.get().uri(authBaseUrl + "/api/auth/internal/users/role/" + request.getRole()).retrieve().bodyToMono(Map[].class).block();
        if (users == null || users.length == 0) {
            return List.of();
        }

        List<Notification> notifications = java.util.Arrays.stream(users).map(u -> {
            Notification n = new Notification();
            n.setRecipientId(Long.valueOf(String.valueOf(u.get("id"))));
            n.setActorId(request.getActorId());
            n.setType(request.getType());
            n.setTitle(request.getTitle());
            n.setMessage(request.getMessage());
            n.setRelatedId(request.getRelatedId());
            n.setRelatedType(request.getRelatedType());
            return n;
        }).toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);
        logAudit(request.getActorId(), "BROADCAST_NOTIFICATION_BY_ROLE", "NOTIFICATION", request.getRole(),
                "recipients=" + saved.size());
        return saved;
    }

    public List<Notification> byRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public Map<String, Long> unreadCount(Long recipientId) {
        return Map.of("count", notificationRepository.countByRecipientIdAndReadFalse(recipientId));
    }

    public Notification markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public Map<String, Long> markAllRead(Long recipientId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalse(recipientId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return Map.of("updated", (long) unread.size());
    }

    public Map<String, Long> deleteRead(Long recipientId) {
        List<Notification> all = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        List<Notification> read = all.stream().filter(Notification::isRead).toList();
        notificationRepository.deleteAll(read);
        return Map.of("deleted", (long) read.size());
    }

    public void deleteOne(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    private void logAudit(Long actorUserId, String action, String entityType, String entityId, String details) {
        try {
            Map<String, Object> payload = Map.of(
                    "actorUserId", actorUserId,
                    "action", action,
                    "entityType", entityType,
                    "entityId", entityId,
                    "details", details
            );
            webClient.post().uri(authBaseUrl + "/api/auth/internal/audit-log").bodyValue(payload).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // best effort
        }
    }
}


