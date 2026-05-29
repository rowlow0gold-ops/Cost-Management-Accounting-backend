package com.demo.cost.security;

import com.demo.cost.domain.AuditEvent;
import com.demo.cost.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditLog {

    private final AuditEventRepository repo;

    public void record(String email, String action, boolean success, String details) {
        String ip = null;
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (IllegalStateException ignored) {}

        repo.save(AuditEvent.builder()
                .email(email)
                .action(action)
                .ip(ip)
                .success(success)
                .details(details != null && details.length() > 500 ? details.substring(0, 500) : details)
                .build());
    }
}
