package com.inkwell.newsletter_subscription_service;

import com.inkwell.newsletter_subscription_service.dto.SubscribeRequest;
import com.inkwell.newsletter_subscription_service.exception.BadRequestException;
import com.inkwell.newsletter_subscription_service.model.Subscriber;
import com.inkwell.newsletter_subscription_service.model.SubscriberStatus;
import com.inkwell.newsletter_subscription_service.repository.SubscriberRepository;
import com.inkwell.newsletter_subscription_service.service.NewsletterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceUnitTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private NewsletterService newsletterService;

    @Test
    void subscribeShouldCreatePendingSubscriber() {
        SubscribeRequest request = new SubscribeRequest();
        request.setEmail("reader1@example.com");

        when(subscriberRepository.findByEmail("reader1@example.com")).thenReturn(Optional.empty());
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscriber created = newsletterService.subscribe(request);

        assertEquals(SubscriberStatus.PENDING, created.getStatus());
        assertNotNull(created.getToken());
    }

    @Test
    void confirmShouldFailWhenTokenExpired() {
        Subscriber subscriber = new Subscriber();
        subscriber.setToken("abc");
        subscriber.setTokenExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(subscriberRepository.findByToken("abc")).thenReturn(Optional.of(subscriber));

        assertThrows(BadRequestException.class, () -> newsletterService.confirm("abc"));
    }
}
