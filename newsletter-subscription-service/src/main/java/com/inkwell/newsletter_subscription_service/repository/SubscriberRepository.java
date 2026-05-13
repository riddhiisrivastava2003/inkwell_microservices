package com.inkwell.newsletter_subscription_service.repository;

import com.inkwell.newsletter_subscription_service.model.Subscriber;
import com.inkwell.newsletter_subscription_service.model.SubscriberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByEmail(String email);

    Optional<Subscriber> findByToken(String token);
    Optional<Subscriber> findByUserId(Long userId);

    List<Subscriber> findByStatus(SubscriberStatus status);
}
