package com.noaats.cost.controller;

import com.noaats.cost.domain.CostAllocation;
import com.noaats.cost.dto.CostDtos.*;
import com.noaats.cost.dto.CostDtos.TimeSeriesPoint;
import com.noaats.cost.service.CostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;

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

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CostAllocation transfer(@RequestBody TransferRequest req) {
        return costService.transfer(req);
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
