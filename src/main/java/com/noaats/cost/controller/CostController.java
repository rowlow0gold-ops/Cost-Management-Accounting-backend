package com.noaats.cost.controller;

import com.noaats.cost.domain.CostAllocation;
import com.noaats.cost.dto.CostDtos.*;
import com.noaats.cost.dto.CostDtos.TimeSeriesPoint;
import com.noaats.cost.repository.CostAllocationRepository;
import com.noaats.cost.service.CostService;
import com.noaats.cost.service.TransferImportService;
import com.noaats.cost.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cost")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;
    private final TransferImportService transferImport;
    private final CostAllocationRepository allocationRepo;

    @GetMapping("/aggregate")
    public List<CostAggregateRow> aggregate(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "PROJECT") String level,
            @RequestParam(defaultValue = "MONTHLY") String scope,
            @RequestParam(defaultValue = "false") boolean breakdownByMonth) {
        return costService.aggregate(yearMonth, level, scope, breakdownByMonth);
    }

    @PostMapping("/allocate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<CostAllocation> allocate(@RequestBody AllocateRequest req) {
        return costService.allocate(req);
    }

    @GetMapping("/allocations")
    public Page<CostAllocation> allocations(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword) {
        return allocationRepo.searchAllocations(yearMonth, keyword,
            PageHelper.of(page, size, sortBy, sortDir));
    }

    @GetMapping("/transfers")
    public Object transfers(
            @RequestParam String yearMonth,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String keyword) {
        if (page != null) {
            return allocationRepo.searchTransfers(yearMonth, keyword,
                PageHelper.of(page, size, sortBy, sortDir));
        }
        return costService.transfers(yearMonth);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CostAllocation transfer(@RequestBody TransferRequest req) {
        return costService.transfer(req);
    }

    @PostMapping("/transfers/validate")
    public java.util.Map<String, Object> validateTransfers(@RequestParam("file") MultipartFile file) throws java.io.IOException {
        int valid = transferImport.validateExcel(file);
        return java.util.Map.of("valid", valid);
    }

    @PostMapping("/transfers/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public java.util.Map<String, Object> importTransfers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "MERGE") String mode) throws java.io.IOException {
        int count = transferImport.importFromExcel(file, mode);
        return java.util.Map.of("message", count + "건 등록 완료", "imported", count);
    }

    @GetMapping("/variance")
    public List<VarianceRow> variance(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "MONTHLY") String scope) {
        return costService.variance(yearMonth, scope);
    }

    @GetMapping("/variance-timeseries")
    public List<TimeSeriesPoint> varianceTimeSeries(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "MONTHLY") String scope) {
        return costService.varianceTimeSeries(yearMonth, scope);
    }
}
