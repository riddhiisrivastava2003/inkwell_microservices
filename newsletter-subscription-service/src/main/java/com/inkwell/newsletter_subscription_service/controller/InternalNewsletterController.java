package com.inkwell.newsletter_subscription_service.controller;

import com.inkwell.newsletter_subscription_service.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/newsletter")
@RequiredArgsConstructor
public class InternalNewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/new-post")
    public ResponseEntity<Map<String, Object>> newPost(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(newsletterService.sendNewPostNotification(payload));
    }
}
