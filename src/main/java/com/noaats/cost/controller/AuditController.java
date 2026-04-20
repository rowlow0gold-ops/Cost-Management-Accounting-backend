package com.noaats.cost.controller;

import com.noaats.cost.domain.AuditLog;
import com.noaats.cost.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository repo;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> recent() {
        return repo.findTop100ByOrderByCreatedAtDesc();
    }
}
