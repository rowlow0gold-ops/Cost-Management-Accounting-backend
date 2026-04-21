package com.noaats.cost.controller;

import com.noaats.cost.domain.Timesheet;
import com.noaats.cost.dto.CostDtos.TimesheetRequest;
import com.noaats.cost.repository.TimesheetRepository;
import com.noaats.cost.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService service;
    private final TimesheetRepository repo;

    @GetMapping
    public List<Timesheet> list(@RequestParam(required = false) String status) {
        if (status == null) return repo.findAllByOrderByWorkDateDescIdDesc();
        return repo.findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status.valueOf(status.toUpperCase()));
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

    // ---- Bulk actions ----
    @PostMapping("/bulk/submit")
    public ResponseEntity<?> bulkSubmit() {
        int count = service.bulkSubmit();
        return ResponseEntity.ok(Map.of("count", count, "message", count + "건 제출됨"));
    }

    @PostMapping("/bulk/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> bulkApprove(Principal principal) {
        String who = principal == null ? "system" : principal.getName();
        int count = service.bulkApprove(who);
        return ResponseEntity.ok(Map.of("count", count, "message", count + "건 승인됨"));
    }

    @PostMapping("/bulk/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> bulkReject(Principal principal) {
        String who = principal == null ? "system" : principal.getName();
        int count = service.bulkReject(who);
        return ResponseEntity.ok(Map.of("count", count, "message", count + "건 반려됨"));
    }

    @DeleteMapping("/bulk/draft")
    public ResponseEntity<?> bulkDeleteDraft() {
        int count = service.bulkDeleteDraft();
        return ResponseEntity.ok(Map.of("count", count, "message", count + "건 삭제됨"));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> validateExcel(@RequestParam("file") MultipartFile file) {
        try {
            int count = service.validateExcel(file);
            return ResponseEntity.ok(Map.of("valid", count, "message", count + "건 검증 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "MERGE") String mode) {
        try {
            int count = service.importFromExcel(file, mode);
            return ResponseEntity.ok(Map.of("imported", count, "message", count + "건이 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
