package com.inkwell.notification_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleBroadcastRequest {
    private String role;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
}
