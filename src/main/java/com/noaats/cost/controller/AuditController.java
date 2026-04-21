package com.noaats.cost.controller;

import com.noaats.cost.domain.AuditLog;
import com.noaats.cost.repository.AuditLogRepository;
import com.noaats.cost.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository repo;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLog> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword) {
        String sort = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        return repo.search(keyword, PageHelper.of(page, size, sort, sortDir));
    }
}
