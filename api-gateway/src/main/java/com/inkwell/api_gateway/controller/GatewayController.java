package com.inkwell.api_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-gateway",
                "time", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/routes")
    public ResponseEntity<Map<String, List<String>>> routes() {
        return ResponseEntity.ok(Map.of(
                "routes", List.of(
                        "/auth/** -> auth-service",
                        "/posts/**,/categories/**,/tags/** -> post-category-tag-service",
                        "/comments/** -> comment-like-service",
                        "/media/** -> media-service",
                        "/newsletter/** -> newsletter-subscription-service",
                        "/notifications/** -> notification-service"
                )
        ));
    }
}
