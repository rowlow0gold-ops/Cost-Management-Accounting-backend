package com.noaats.cost.controller;

import com.noaats.cost.domain.Timesheet;
import com.noaats.cost.dto.CostDtos.TimesheetRequest;
import com.noaats.cost.repository.TimesheetRepository;
import com.noaats.cost.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService service;
    private final TimesheetRepository repo;

    @GetMapping
    public List<Timesheet> list(@RequestParam(required = false) String status) {
        if (status == null) return repo.findAll();
        return repo.findByStatus(Timesheet.Status.valueOf(status.toUpperCase()));
    }

    @PostMapping
    public Timesheet create(@RequestBody TimesheetRequest req) { return service.create(req); }

    @PostMapping("/{id}/submit")
    public Timesheet submit(@PathVariable Long id) { return service.submit(id); }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Timesheet approve(@PathVariable Long id, Principal principal) {
        return service.approve(id, principal == null ? "system" : principal.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Timesheet reject(@PathVariable Long id, Principal principal) {
        return service.reject(id, principal == null ? "system" : principal.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
