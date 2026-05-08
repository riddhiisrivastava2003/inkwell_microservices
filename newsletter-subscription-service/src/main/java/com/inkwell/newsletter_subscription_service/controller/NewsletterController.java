package com.inkwell.newsletter_subscription_service.controller;

import com.inkwell.newsletter_subscription_service.dto.CampaignRequest;
import com.inkwell.newsletter_subscription_service.dto.SubscribeRequest;
import com.inkwell.newsletter_subscription_service.model.Subscriber;
import com.inkwell.newsletter_subscription_service.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<Subscriber> subscribe(@Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.ok(newsletterService.subscribe(request));
    }

    @GetMapping("/confirm")
    public ResponseEntity<Subscriber> confirm(@RequestParam String token) {
        return ResponseEntity.ok(newsletterService.confirm(token));
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<Subscriber> unsubscribe(@RequestParam String token) {
        return ResponseEntity.ok(newsletterService.unsubscribe(token));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<List<Subscriber>> all(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(newsletterService.all());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(newsletterService.count());
    }

    @PostMapping("/campaign")
    public ResponseEntity<Map<String, Object>> campaign(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                        @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                        @RequestBody CampaignRequest request) {
        requireAnyRole(role, "ADMIN");
        return ResponseEntity.ok(newsletterService.sendCampaign(request, userId));
    }

    @PutMapping("/subscribers/{subscriberId}/activate")
    public ResponseEntity<Subscriber> activateSubscriber(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                         @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                         @RequestParam(value = "userId", required = false) Long userIdParam,
                                                         @PathVariable Long subscriberId) {
        Long effectiveUserId = userId != null ? userId : userIdParam;
        requireAuthenticated(effectiveUserId);
        return ResponseEntity.ok(newsletterService.activateSubscriber(subscriberId, effectiveUserId));
    }

    @PutMapping("/subscribers/{subscriberId}/deactivate")
    public ResponseEntity<Subscriber> deactivateSubscriber(@RequestHeader(value = "X-User-Role", required = false) String role,
                                                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                           @PathVariable Long subscriberId) {
        requireAnyRole(role, "ADMIN");
        requireAuthenticated(userId);
        return ResponseEntity.ok(newsletterService.deactivateSubscriber(subscriberId, userId));
    }

    @PutMapping("/me/deactivate")
    public ResponseEntity<Subscriber> deactivateOwnSubscription(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireAuthenticated(userId);
        return ResponseEntity.ok(newsletterService.deactivateByUserId(userId));
    }

    @PostMapping("/internal/new-post")
    public ResponseEntity<Map<String, Object>> newPost(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(newsletterService.sendNewPostNotification(payload));
    }

    private void requireAnyRole(String role, String... allowed) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing user role");
        }
        String normalizedRole = role.trim();
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        for (String allow : allowed) {
            if (allow.equalsIgnoreCase(normalizedRole)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }
    }
}
