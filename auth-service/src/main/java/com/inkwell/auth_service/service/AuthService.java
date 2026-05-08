package com.inkwell.auth_service.service;

import com.inkwell.auth_service.dto.*;
import com.inkwell.auth_service.audit.AuditLogService;
import com.inkwell.auth_service.exception.BadRequestException;
import com.inkwell.auth_service.model.PasswordResetToken;
import com.inkwell.auth_service.model.User;
import com.inkwell.auth_service.model.UserFollow;
import com.inkwell.auth_service.model.UserRole;
import com.inkwell.auth_service.repository.PasswordResetTokenRepository;
import com.inkwell.auth_service.repository.UserFollowRepository;
import com.inkwell.auth_service.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final JavaMailSender mailSender;

    @Value("${auth.admin.registration-key:}")
    private String adminRegistrationKey;
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;
    @Value("${auth.password-reset.expiry-minutes:30}")
    private long passwordResetExpiryMinutes;
    @Value("${auth.mail.from:}")
    private String mailFrom;

    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Use admin registration endpoint for admin account");
        }
        return registerInternal(request, request.getRole() == null ? UserRole.READER : request.getRole());
    }

    public AuthResponse registerAdmin(RegisterRequest request) {
        if (adminRegistrationKey == null || adminRegistrationKey.isBlank()) {
            throw new BadRequestException("Admin registration is disabled");
        }
        if (!adminRegistrationKey.equals(request.getAdminKey())) {
            throw new BadRequestException("Invalid admin registration key");
        }
        return registerInternal(request, UserRole.ADMIN);
    }

    private AuthResponse registerInternal(RegisterRequest request, UserRole targetRole) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(targetRole);
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return toAuthResponse(saved, token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is suspended");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return toAuthResponse(user, token);
    }

    public UserResponse getById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        return toUserResponse(user);
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    public List<UserResponse> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.findByUsernameContainingIgnoreCase(query)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public List<UserResponse> usersByRole(UserRole role) {
        return userRepository.findByRole(role).stream().map(this::toUserResponse).toList();
    }

    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        user.setFullName(request.getFullName());
        user.setBio(request.getBio());
        user.setAvatarUrl(request.getAvatarUrl());
        return toUserResponse(userRepository.save(user));
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetExpiryMinutes));
        passwordResetTokenRepository.save(resetToken);

        sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (token.getUsedAt() != null) {
            throw new BadRequestException("Reset token already used");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.deleteByUserId(user.getId());
    }

    public UserResponse changeRole(Long userId, ChangeRoleRequest request, Long actorUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        String oldRole = user.getRole().name();
        user.setRole(request.getRole());
        User saved = userRepository.save(user);
        auditLogService.log(actorUserId, "CHANGE_ROLE", "USER", String.valueOf(userId),
                "Changed role from " + oldRole + " to " + request.getRole());
        return toUserResponse(saved);
    }

    public UserResponse setActive(Long userId, boolean active, Long actorUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        user.setActive(active);
        User saved = userRepository.save(user);
        auditLogService.log(actorUserId, active ? "REACTIVATE_USER" : "SUSPEND_USER", "USER", String.valueOf(userId),
                "Set active=" + active);
        return toUserResponse(saved);
    }

    public void deleteUser(Long userId, Long actorUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        userFollowRepository.deleteByFollowerIdOrFolloweeId(userId, userId);
        userRepository.delete(user);
        auditLogService.log(actorUserId, "DELETE_USER", "USER", String.valueOf(userId), "User deleted permanently");
    }

    public Map<String, Object> follow(Long followerId, Long followeeId) {
        if (followerId == null) {
            throw new BadRequestException("Missing follower identity");
        }
        if (followeeId == null) {
            throw new BadRequestException("followeeId is required");
        }
        if (followerId.equals(followeeId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        userRepository.findById(followerId).orElseThrow(() -> new BadRequestException("Follower not found"));
        userRepository.findById(followeeId).orElseThrow(() -> new BadRequestException("Author not found"));

        if (!userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            userFollowRepository.save(follow);
            auditLogService.log(followerId, "FOLLOW_AUTHOR", "USER", String.valueOf(followeeId), "started following");
        }

        return Map.of(
                "following", true,
                "followerCount", userFollowRepository.countByFolloweeId(followeeId)
        );
    }

    public Map<String, Object> unfollow(Long followerId, Long followeeId) {
        if (followerId == null) {
            throw new BadRequestException("Missing follower identity");
        }
        if (followeeId == null) {
            throw new BadRequestException("followeeId is required");
        }

        if (userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            userFollowRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
            auditLogService.log(followerId, "UNFOLLOW_AUTHOR", "USER", String.valueOf(followeeId), "stopped following");
        }

        return Map.of(
                "following", false,
                "followerCount", userFollowRepository.countByFolloweeId(followeeId)
        );
    }

    public Map<String, Object> followStatus(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            return Map.of(
                    "following", false,
                    "followerCount", followeeId == null ? 0 : userFollowRepository.countByFolloweeId(followeeId)
            );
        }
        return Map.of(
                "following", userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId),
                "followerCount", userFollowRepository.countByFolloweeId(followeeId)
        );
    }

    public List<Long> followerIds(Long followeeId) {
        return userFollowRepository.findByFolloweeId(followeeId)
                .stream()
                .map(UserFollow::getFollowerId)
                .distinct()
                .toList();
    }

    public List<Long> followingIds(Long followerId) {
        if (followerId == null) {
            return List.of();
        }
        return userFollowRepository.findByFollowerId(followerId)
                .stream()
                .map(UserFollow::getFolloweeId)
                .distinct()
                .toList();
    }

    public Claims validateToken(String token) {
        return jwtService.validateToken(token);
    }

    public String generateTokenForUser(User user) {
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        String body = "We received a request to reset your InkWell password.\n\n"
                + "Reset link: " + resetUrl + "\n\n"
                + "This link expires in " + passwordResetExpiryMinutes + " minutes.\n"
                + "If you did not request this, you can ignore this email.";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom.trim());
            }
            message.setTo(toEmail);
            message.setSubject("InkWell password reset");
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }
}
