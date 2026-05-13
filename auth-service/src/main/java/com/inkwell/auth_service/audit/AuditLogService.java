package com.inkwell.auth_service.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    public void log(Long actorUserId, String action, String entityType, String entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        repository.save(auditLog);
    }

    public List<AuditLog> all() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
