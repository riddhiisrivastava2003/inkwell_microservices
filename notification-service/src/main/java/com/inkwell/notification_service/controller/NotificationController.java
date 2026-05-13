package com.inkwell.notification_service.controller;

import com.inkwell.notification_service.model.Notification;
import com.inkwell.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Notification> send(@RequestBody com.inkwell.notification_service.dto.NotificationRequest request) {
        return ResponseEntity.ok(notificationService.send(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Notification>> sendBulk(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                       @RequestBody com.inkwell.notification_service.dto.BulkNotificationRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(notificationService.sendBulk(request));
    }

    @PostMapping("/internal/bulk")
    public ResponseEntity<List<Notification>> sendBulkInternal(@RequestBody com.inkwell.notification_service.dto.BulkNotificationRequest request) {
        return ResponseEntity.ok(notificationService.sendBulk(request));
    }

    @PostMapping("/broadcast/role")
    public ResponseEntity<List<Notification>> broadcastByRole(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                              @RequestBody com.inkwell.notification_service.dto.RoleBroadcastRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(notificationService.sendByRole(request));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> byRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.byRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.unreadCount(recipientId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<Map<String, Long>> markAllRead(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.markAllRead(recipientId));
    }

    @DeleteMapping("/recipient/{recipientId}/read")
    public ResponseEntity<Map<String, Long>> deleteRead(@PathVariable Long recipientId) {
        return ResponseEntity.ok(notificationService.deleteRead(recipientId));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable Long notificationId) {
        notificationService.deleteOne(notificationId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
