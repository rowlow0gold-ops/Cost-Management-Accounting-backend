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
