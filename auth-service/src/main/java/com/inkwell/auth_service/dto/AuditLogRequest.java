package com.inkwell.auth_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditLogRequest {
    private Long actorUserId;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
}
