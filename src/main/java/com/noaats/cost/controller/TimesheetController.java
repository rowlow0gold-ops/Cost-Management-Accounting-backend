package com.noaats.cost.controller;

import com.noaats.cost.domain.Timesheet;
import com.noaats.cost.dto.CostDtos.TimesheetRequest;
import com.noaats.cost.repository.TimesheetRepository;
import com.noaats.cost.service.TimesheetService;
import com.noaats.cost.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService service;
    private final TimesheetRepository repo;

    @GetMapping
    public Page<Timesheet> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String keyword) {
        Timesheet.Status st = null;
        if (status != null && !status.isBlank()) {
            st = Timesheet.Status.valueOf(status.toUpperCase());
        }
        String sort = (sortBy != null && !sortBy.isBlank()) ? sortBy : "workDate";
        return repo.search(st, keyword, PageHelper.of(page, size, sort, sortDir));
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
