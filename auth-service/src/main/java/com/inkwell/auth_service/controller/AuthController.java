package com.inkwell.auth_service.controller;

import com.inkwell.auth_service.dto.*;
import com.inkwell.auth_service.audit.AuditLog;
import com.inkwell.auth_service.audit.AuditLogService;
import com.inkwell.auth_service.exception.BadRequestException;
import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserRole;
import com.inkwell.auth_service.repository.UserRepository;
import com.inkwell.auth_service.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final WebClient webClient;

    @Value("${services.post.base-url:http://localhost:8082}")
    private String postBaseUrl;

    @Value("${services.comment.base-url:http://localhost:8083}")
    private String commentBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerAdmin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "If the email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam(required = false) String token,
                                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if ((token == null || token.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token is required");
        }
        Claims claims = authService.validateToken(token);
        return ResponseEntity.ok(Map.of(
                "userId", claims.get("userId"),
                "email", claims.getSubject(),
                "role", claims.get("role")
        ));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> users() {
        return ResponseEntity.ok(authService.getAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<List<UserResponse>> search(@RequestParam String username) {
        return ResponseEntity.ok(authService.searchUsers(username));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<UserResponse> userById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getById(userId));
    }

    @GetMapping("/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> usersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(authService.usersByRole(role));
    }

    @GetMapping("/internal/users/role/{role}")
    public ResponseEntity<List<UserResponse>> internalUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(authService.usersByRole(role));
    }

    @GetMapping("/internal/users/{userId}/followers")
    public ResponseEntity<Map<String, Object>> internalFollowerIds(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("followerIds", authService.followerIds(userId)));
    }

    @GetMapping("/internal/users/{userId}/following")
    public ResponseEntity<Map<String, Object>> internalFollowingIds(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of("followingIds", authService.followingIds(userId)));
    }

    @PostMapping("/internal/audit-log")
    public ResponseEntity<Map<String, String>> internalAuditLog(@RequestBody AuditLogRequest request) {
        auditLogService.log(
                request.getActorUserId(),
                request.getAction(),
                request.getEntityType(),
                request.getEntityId(),
                request.getDetails()
        );
        return ResponseEntity.ok(Map.of("message", "audit-logged"));
    }

    @PutMapping("/users/{userId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable Long userId, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/users/{userId}/password")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long userId,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long userId,
                                                   @RequestParam(required = false) Long actorUserId,
                                                   @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(authService.changeRole(userId, request, actorUserId));
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> setStatus(@PathVariable Long userId,
                                                  @RequestParam boolean active,
                                                  @RequestParam(required = false) Long actorUserId) {
        return ResponseEntity.ok(authService.setActive(userId, active, actorUserId));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId,
                                                          @RequestParam(required = false) Long actorUserId) {
        authService.deleteUser(userId, actorUserId);
        return ResponseEntity.ok(Map.of("message", "User deleted permanently"));
    }

    @PostMapping("/users/{userId}/follow")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<Map<String, Object>> follow(@PathVariable Long userId,
                                                      @RequestHeader(value = "X-User-Id", required = false) Long followerId) {
        return ResponseEntity.ok(authService.follow(followerId, userId));
    }

    @DeleteMapping("/users/{userId}/follow")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<Map<String, Object>> unfollow(@PathVariable Long userId,
                                                        @RequestHeader(value = "X-User-Id", required = false) Long followerId) {
        return ResponseEntity.ok(authService.unfollow(followerId, userId));
    }

    @GetMapping("/users/{userId}/follow-status")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<Map<String, Object>> followStatus(@PathVariable Long userId,
                                                            @RequestHeader(value = "X-User-Id", required = false) Long followerId) {
        return ResponseEntity.ok(authService.followStatus(followerId, userId));
    }

    @GetMapping("/users/{userId}/following")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR','READER')")
    public ResponseEntity<Map<String, Object>> following(@PathVariable Long userId,
                                                         @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        if (requesterId == null || (!requesterId.equals(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view following list");
        }
        return ResponseEntity.ok(Map.of("followingIds", authService.followingIds(userId)));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> auditLogs() {
        return ResponseEntity.ok(auditLogService.all());
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<AuthResponse> oauthSuccess(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            throw new BadRequestException("OAuth2 login session not found");
        }
        String email = String.valueOf(oauth2User.getAttributes().getOrDefault("email", ""));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found"));
        String token = authService.generateTokenForUser(user);
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .build());
    }

    @GetMapping("/admin/platform-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> platformAnalytics() {
        long totalUsers = userRepository.count();
        Object postAnalytics = webClient.get().uri(postBaseUrl + "/api/admin/analytics").retrieve().bodyToMono(Object.class).block();
        Map commentCount = webClient.get().uri(commentBaseUrl + "/api/comments/count-total").retrieve().bodyToMono(Map.class).block();
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "postAnalytics", postAnalytics == null ? Map.of() : postAnalytics,
                "totalComments", commentCount == null ? 0 : commentCount.get("count")
        ));
    }
}


