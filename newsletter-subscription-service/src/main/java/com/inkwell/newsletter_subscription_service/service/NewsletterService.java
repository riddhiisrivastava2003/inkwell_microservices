package com.inkwell.newsletter_subscription_service.service;

import com.inkwell.newsletter_subscription_service.dto.CampaignRequest;
import com.inkwell.newsletter_subscription_service.dto.SubscribeRequest;
import com.inkwell.newsletter_subscription_service.exception.BadRequestException;
import com.inkwell.newsletter_subscription_service.model.Subscriber;
import com.inkwell.newsletter_subscription_service.model.SubscriberStatus;
import com.inkwell.newsletter_subscription_service.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    private final SubscriberRepository subscriberRepository;
    private final JavaMailSender mailSender;
    private final WebClient webClient;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${services.auth.base-url}")
    private String authBaseUrl;

    @Value("${newsletter.campaign.strict-preference:false}")
    private boolean strictPreferenceFilter;

    @Value("${newsletter.mail.from:}")
    private String mailFrom;

    @Transactional
    public Subscriber subscribe(SubscribeRequest request) {
        try {
            Subscriber saved = upsertSubscriber(request);
            sendConfirmationEmail(saved);
            sendWelcomePendingEmail(saved);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Handles concurrent subscribe requests for the same email.
            Subscriber existing = subscriberRepository.findByEmail(request.getEmail()).orElseThrow(() -> ex);
            existing.setUserId(request.getUserId());
            existing.setFullName(request.getFullName());
            existing.setPreferences(request.getPreferences());
            existing.setToken(UUID.randomUUID().toString());
            existing.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
            existing.setStatus(SubscriberStatus.PENDING);
            Subscriber saved = subscriberRepository.save(existing);
            sendConfirmationEmail(saved);
            sendWelcomePendingEmail(saved);
            return saved;
        }
    }

    public Subscriber confirm(String token) {
        Subscriber subscriber = subscriberRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token"));
        if (subscriber.getTokenExpiresAt() != null && subscriber.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Confirmation token expired");
        }

        subscriber.setStatus(SubscriberStatus.ACTIVE);
        Subscriber saved = subscriberRepository.save(subscriber);
        sendWelcomeEmail(saved);
        return saved;
    }

    public Subscriber unsubscribe(String token) {
        Subscriber subscriber = subscriberRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        return subscriberRepository.save(subscriber);
    }

    public Subscriber activateSubscriber(Long subscriberId, Long actorUserId) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new BadRequestException("Subscriber not found"));

        subscriber.setStatus(SubscriberStatus.ACTIVE);
        subscriber.setUnsubscribedAt(null);
        Subscriber saved = subscriberRepository.save(subscriber);

        logAudit(actorUserId, "ACTIVATE_SUBSCRIBER", "NEWSLETTER_SUBSCRIBER", String.valueOf(subscriberId),
                "email=" + subscriber.getEmail());
        return saved;
    }

    public Subscriber deactivateSubscriber(Long subscriberId, Long actorUserId) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new BadRequestException("Subscriber not found"));

        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        Subscriber saved = subscriberRepository.save(subscriber);

        logAudit(actorUserId, "DEACTIVATE_SUBSCRIBER", "NEWSLETTER_SUBSCRIBER", String.valueOf(subscriberId),
                "email=" + subscriber.getEmail());
        return saved;
    }

    public Subscriber deactivateByUserId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("Missing user identity");
        }
        Subscriber subscriber = subscriberRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Subscriber not found for this user"));
        subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        return subscriberRepository.save(subscriber);
    }

    public List<Subscriber> all() {
        return subscriberRepository.findAll();
    }

    public Map<String, Long> count() {
        long pending = subscriberRepository.findByStatus(SubscriberStatus.PENDING).size();
        long active = subscriberRepository.findByStatus(SubscriberStatus.ACTIVE).size();
        long unsubscribed = subscriberRepository.findByStatus(SubscriberStatus.UNSUBSCRIBED).size();
        return Map.of(
                "pending", pending,
                "active", active,
                "unsubscribed", unsubscribed,
                "total", pending + active + unsubscribed
        );
    }

    public Map<String, Object> sendCampaign(CampaignRequest request, Long actorUserId) {
        List<Subscriber> active = subscriberRepository.findByStatus(SubscriberStatus.ACTIVE);
        List<Subscriber> targets;
        String keywordRaw = request.getPreferenceKeyword();
        String keyword = keywordRaw == null ? "" : keywordRaw.trim().toLowerCase(Locale.ROOT);

        if (keyword.isBlank()) {
            targets = active;
        } else {
            targets = active.stream()
                    .filter(s -> s.getPreferences() != null && s.getPreferences().toLowerCase(Locale.ROOT).contains(keyword))
                    .toList();
            if (targets.isEmpty() && !strictPreferenceFilter) {
                targets = active;
            }
        }

        log.info("Campaign target resolution -> active={}, keyword='{}', strictPreferenceFilter={}, targets={}",
                active.size(), keyword, strictPreferenceFilter, targets.size());

        int sent = 0;
        int failed = 0;
        for (Subscriber subscriber : targets) {
            boolean ok = sendEmail(
                    subscriber.getEmail(),
                    request.getSubject(),
                    request.getContent() + "\n\n" + unsubscribeFooter(subscriber)
            );
            if (ok) {
                sent++;
            } else {
                failed++;
            }
        }
        logAudit(actorUserId, "SEND_NEWSLETTER_CAMPAIGN", "NEWSLETTER", "campaign",
                "subject=" + request.getSubject() + ", recipients=" + targets.size() + ", sent=" + sent + ", failed=" + failed);
        return Map.of(
                "subject", request.getSubject(),
                "recipients", targets.size(),
                "sent", sent,
                "failed", failed
        );
    }

    public Map<String, Object> sendNewPostNotification(Map<String, Object> payload) {
        List<Subscriber> active = subscriberRepository.findByStatus(SubscriberStatus.ACTIVE);
        String title = String.valueOf(payload.getOrDefault("title", "New post published"));
        String slug = String.valueOf(payload.getOrDefault("slug", ""));
        int sent = 0;
        int failed = 0;
        for (Subscriber subscriber : active) {
            String body = "A new post is live on InkWell: " + title + "\n"
                    + "Read now: " + appBaseUrl + "/posts/slug/" + slug + "\n\n"
                    + unsubscribeFooter(subscriber);
            boolean ok = sendEmail(subscriber.getEmail(), "InkWell: New post published", body);
            if (ok) {
                sent++;
            } else {
                failed++;
            }
        }
        log.info("New post notification for '{}' targeted {} subscribers (sent={}, failed={})", title, active.size(), sent, failed);
        return Map.of(
                "recipients", active.size(),
                "sent", sent,
                "failed", failed,
                "postId", payload.get("postId")
        );
    }

    private void sendConfirmationEmail(Subscriber subscriber) {
        String confirmUrl = appBaseUrl + "/newsletter/confirm?token=" + subscriber.getToken();
        String body = "Confirm your InkWell newsletter subscription:\n" + confirmUrl;
        sendEmail(subscriber.getEmail(), "Confirm your InkWell subscription", body);
    }

    private Subscriber upsertSubscriber(SubscribeRequest request) {
        Subscriber subscriber = subscriberRepository.findByEmail(request.getEmail()).orElse(new Subscriber());
        subscriber.setEmail(request.getEmail());
        subscriber.setUserId(request.getUserId());
        subscriber.setFullName(request.getFullName());
        subscriber.setPreferences(request.getPreferences());
        subscriber.setToken(UUID.randomUUID().toString());
        subscriber.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
        subscriber.setStatus(SubscriberStatus.PENDING);
        return subscriberRepository.save(subscriber);
    }

    private void sendWelcomeEmail(Subscriber subscriber) {
        String body = "Welcome to InkWell newsletter.\n\n" + unsubscribeFooter(subscriber);
        sendEmail(subscriber.getEmail(), "Welcome to InkWell", body);
    }

    private void sendWelcomePendingEmail(Subscriber subscriber) {
        String body = "Thanks for subscribing to InkWell.\n"
                + "Please confirm your subscription from your inbox to activate updates.\n\n"
                + "If you did not request this, you can ignore this email.";
        sendEmail(subscriber.getEmail(), "Thanks for subscribing to InkWell", body);
    }

    private boolean sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom.trim());
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send mail to {} with subject '{}': {}", to, subject, ex.getMessage());
            return false;
        }
    }

    private String unsubscribeFooter(Subscriber subscriber) {
        return "Unsubscribe: " + appBaseUrl + "/newsletter/unsubscribe?token=" + subscriber.getToken();
    }

    private void logAudit(Long actorUserId, String action, String entityType, String entityId, String details) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("actorUserId", actorUserId);
            payload.put("action", action);
            payload.put("entityType", entityType);
            payload.put("entityId", entityId);
            payload.put("details", details);
            webClient.post().uri(authBaseUrl + "/api/auth/internal/audit-log").bodyValue(payload).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // best effort
        }
    }
}


