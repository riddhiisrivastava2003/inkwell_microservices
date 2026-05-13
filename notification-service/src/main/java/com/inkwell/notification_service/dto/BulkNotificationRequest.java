package com.inkwell.notification_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkNotificationRequest {
    private List<Long> recipientIds;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
}
