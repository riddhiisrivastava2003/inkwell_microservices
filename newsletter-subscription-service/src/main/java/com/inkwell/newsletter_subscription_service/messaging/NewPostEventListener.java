package com.inkwell.newsletter_subscription_service.messaging;

import com.inkwell.newsletter_subscription_service.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewPostEventListener {

    private final NewsletterService newsletterService;

    @RabbitListener(queues = RabbitConfig.NEW_POST_QUEUE)
    public void onNewPost(Object payload) {
        if (payload instanceof Map<?, ?> mapPayload) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) mapPayload;
            newsletterService.sendNewPostNotification(typed);
            return;
        }

        if (payload instanceof byte[] raw) {
            log.warn("Skipping legacy non-JSON newsletter event payload ({} bytes): {}", raw.length,
                    new String(raw, StandardCharsets.UTF_8));
            return;
        }

        log.warn("Skipping unsupported newsletter event payload type: {}", payload == null ? "null" : payload.getClass().getName());
    }
}
