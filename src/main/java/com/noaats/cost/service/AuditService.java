package com.noaats.cost.service;

import com.noaats.cost.domain.AuditLog;
import com.noaats.cost.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;

    public void log(String action, String entity, Long entityId, String detail) {
        String actor = "system";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof String s) actor = s;
        } catch (Exception ignored) {}

        repo.save(AuditLog.builder()
            .actor(actor)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .detail(detail)
            .createdAt(LocalDateTime.now())
            .build());
    }
}
