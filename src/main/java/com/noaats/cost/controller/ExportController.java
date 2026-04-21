package com.noaats.cost.controller;

import com.noaats.cost.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excel;

    @GetMapping("/aggregate.xlsx")
    public ResponseEntity<byte[]> aggregate(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "PROJECT") String level,
            @RequestParam(defaultValue = "MONTHLY") String scope) throws IOException {
        byte[] body = excel.exportAggregate(yearMonth, level, scope);
        String filename = "aggregate_" + yearMonth + "_" + level + "_" + scope + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/aggregate-all.xlsx")
    public ResponseEntity<byte[]> aggregateAll(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "MONTHLY") String scope) throws IOException {
        byte[] body = excel.exportAggregateAll(yearMonth, scope);
        String filename = "aggregate_" + yearMonth + "_" + scope + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/timesheets.xlsx")
    public ResponseEntity<byte[]> timesheets(
            @RequestParam(required = false) String status) throws IOException {
        byte[] body = excel.exportTimesheets(status);
        String filename = "timesheets" + (status != null ? "_" + status : "") + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/allocations.xlsx")
    public ResponseEntity<byte[]> allocations(
            @RequestParam String yearMonth) throws IOException {
        byte[] body = excel.exportAllocations(yearMonth);
        String filename = "allocations_" + yearMonth + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/transfers.xlsx")
    public ResponseEntity<byte[]> transfers(
            @RequestParam(required = false) String yearMonth) throws IOException {
        byte[] body = excel.exportTransfers(yearMonth);
        String filename = "transfers" + (yearMonth != null ? "_" + yearMonth : "") + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/cost-items.xlsx")
    public ResponseEntity<byte[]> costItems(
            @RequestParam(required = false) String yearMonth) throws IOException {
        byte[] body = excel.exportCostItems(yearMonth);
        String filename = "cost_items" + (yearMonth != null ? "_" + yearMonth : "") + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/audit.xlsx")
    public ResponseEntity<byte[]> audit() throws IOException {
        byte[] body = excel.exportAuditLogs();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_log.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }

    @GetMapping("/variance.xlsx")
    public ResponseEntity<byte[]> variance(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "MONTHLY") String scope) throws IOException {
        byte[] body = excel.exportVariance(yearMonth, scope);
        String filename = "variance_" + yearMonth + "_" + scope + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }
}
