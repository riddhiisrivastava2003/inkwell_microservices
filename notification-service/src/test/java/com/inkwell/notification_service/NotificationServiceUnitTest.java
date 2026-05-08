package com.inkwell.notification_service;

import com.inkwell.notification_service.dto.BulkNotificationRequest;
import com.inkwell.notification_service.dto.NotificationRequest;
import com.inkwell.notification_service.exception.BadRequestException;
import com.inkwell.notification_service.model.Notification;
import com.inkwell.notification_service.repository.NotificationRepository;
import com.inkwell.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendShouldThrowWhenRecipientMissing() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> notificationService.send(request));
        assertEquals("recipientId is required", ex.getMessage());
    }

    @Test
    void sendShouldPersistNotification() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(10L);
        request.setActorId(2L);
        request.setType("NEW_COMMENT");
        request.setTitle("Title");
        request.setMessage("Message");
        request.setRelatedId(101L);
        request.setRelatedType("POST");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = notificationService.send(request);

        assertEquals(10L, saved.getRecipientId());
        assertEquals(2L, saved.getActorId());
        assertEquals("NEW_COMMENT", saved.getType());
    }

    @Test
    void sendBulkShouldThrowWhenRecipientListMissing() {
        BulkNotificationRequest request = new BulkNotificationRequest();
        request.setRecipientIds(List.of());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> notificationService.sendBulk(request));
        assertEquals("recipientIds are required", ex.getMessage());
    }

    @Test
    void sendBulkShouldCreateNotificationsForAllRecipients() {
        BulkNotificationRequest request = new BulkNotificationRequest();
        request.setRecipientIds(List.of(7L, 8L, 9L));
        request.setActorId(2L);
        request.setType("NEW_POST");
        request.setTitle("New post");
        request.setMessage("Check latest post");
        request.setRelatedId(500L);
        request.setRelatedType("POST");

        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Notification> saved = notificationService.sendBulk(request);

        assertEquals(3, saved.size());
        assertEquals(7L, saved.get(0).getRecipientId());
        assertEquals(8L, saved.get(1).getRecipientId());
        assertEquals(9L, saved.get(2).getRecipientId());
    }
}
