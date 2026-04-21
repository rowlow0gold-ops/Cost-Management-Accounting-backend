package com.noaats.cost.service;

import com.noaats.cost.domain.AuditLog;
import com.noaats.cost.domain.CostAllocation;
import com.noaats.cost.domain.CostItem;
import com.noaats.cost.domain.Timesheet;
import com.noaats.cost.dto.CostDtos.CostAggregateRow;
import com.noaats.cost.dto.CostDtos.VarianceRow;
import com.noaats.cost.repository.AuditLogRepository;
import com.noaats.cost.repository.CostAllocationRepository;
import com.noaats.cost.repository.CostItemRepository;
import com.noaats.cost.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final CostService costService;
    private final TimesheetRepository timesheetRepo;
    private final CostAllocationRepository allocationRepo;
    private final CostItemRepository costItemRepo;
    private final AuditLogRepository auditLogRepo;

    public byte[] exportAggregate(String yearMonth, String level, String scope) throws IOException {
        List<CostAggregateRow> rows = costService.aggregate(yearMonth, level, scope);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(level + "_" + yearMonth);
            CellStyle header = headerStyle(wb);
            String[] cols = {"레벨", "코드", "이름", "공수(h)", "직접원가(KRW)"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (CostAggregateRow row : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(row.getLevel());
                dr.createCell(1).setCellValue(row.getKeyCode());
                dr.createCell(2).setCellValue(row.getKeyName());
                dr.createCell(3).setCellValue(row.getHours().doubleValue());
                dr.createCell(4).setCellValue(row.getDirectCost().doubleValue());
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Exports a single workbook with three sheets: 본부, 프로젝트, 직원.
     */
    public byte[] exportAggregateAll(String yearMonth, String scope) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(wb);
            String[][] levels = {
                {"DEPARTMENT", "본부"},
                {"PROJECT",    "프로젝트"},
                {"EMPLOYEE",   "직원"},
            };
            for (String[] entry : levels) {
                String level = entry[0];
                String sheetName = entry[1];
                List<CostAggregateRow> rows = costService.aggregate(yearMonth, level, scope);
                Sheet sh = wb.createSheet(sheetName);
                String[] cols = {"코드", "이름", "공수(h)", "직접원가(KRW)"};
                Row h = sh.createRow(0);
                for (int i = 0; i < cols.length; i++) {
                    Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
                }
                int r = 1;
                for (CostAggregateRow row : rows) {
                    Row dr = sh.createRow(r++);
                    dr.createCell(0).setCellValue(row.getKeyCode());
                    dr.createCell(1).setCellValue(row.getKeyName());
                    dr.createCell(2).setCellValue(row.getHours().doubleValue());
                    dr.createCell(3).setCellValue(row.getDirectCost().doubleValue());
                }
                for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportVariance(String yearMonth, String scope) throws IOException {
        List<VarianceRow> rows = costService.variance(yearMonth, scope);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Variance_" + yearMonth);
            CellStyle header = headerStyle(wb);
            String[] cols = {"코드","프로젝트","예산공수","실적공수","예산원가","실적원가","공수차이","원가차이","차이%"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (VarianceRow row : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(row.getProjectCode());
                dr.createCell(1).setCellValue(row.getProjectName());
                dr.createCell(2).setCellValue(row.getBudgetHours().doubleValue());
                dr.createCell(3).setCellValue(row.getActualHours().doubleValue());
                dr.createCell(4).setCellValue(row.getBudgetCost().doubleValue());
                dr.createCell(5).setCellValue(row.getActualCost().doubleValue());
                dr.createCell(6).setCellValue(row.getHourVariance().doubleValue());
                dr.createCell(7).setCellValue(row.getCostVariance().doubleValue());
                dr.createCell(8).setCellValue(row.getCostVariancePct().doubleValue());
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTimesheets(String status) throws IOException {
        List<Timesheet> rows;
        if (status != null && !status.isBlank()) {
            rows = timesheetRepo.findByStatusOrderByWorkDateDescIdDesc(
                Timesheet.Status.valueOf(status.toUpperCase()));
        } else {
            rows = timesheetRepo.findAllByOrderByWorkDateDescIdDesc();
        }
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("공수");
            CellStyle header = headerStyle(wb);
            // Matches import format: 사번 | 프로젝트코드 | 근무일 | 시간 | 메모 | Action
            String[] cols = {"사번", "프로젝트코드", "근무일", "시간", "메모", "Action"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (Timesheet t : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(t.getEmployee() != null ? t.getEmployee().getEmpNo() : "");
                dr.createCell(1).setCellValue(t.getProject() != null ? t.getProject().getCode() : "");
                dr.createCell(2).setCellValue(t.getWorkDate().toString());
                dr.createCell(3).setCellValue(t.getHours().doubleValue());
                dr.createCell(4).setCellValue(t.getMemo() != null ? t.getMemo() : "");
                dr.createCell(5).setCellValue(t.getStatus().name());
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportAllocations(String yearMonth) throws IOException {
        List<CostAllocation> rows = allocationRepo.findByYearMonth(yearMonth);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("배분결과_" + yearMonth);
            CellStyle header = headerStyle(wb);
            String[] cols = {"회계월", "출처 본부", "대상 프로젝트", "배부기준", "배분액(KRW)", "유형", "메모"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (CostAllocation a : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(a.getYearMonth());
                dr.createCell(1).setCellValue(a.getSourceDepartment() != null ? a.getSourceDepartment().getName() : "");
                dr.createCell(2).setCellValue(a.getTargetProject() != null ? a.getTargetProject().getCode() + " — " + a.getTargetProject().getName() : "");
                dr.createCell(3).setCellValue(a.getBasis().name());
                dr.createCell(4).setCellValue(a.getAmount().doubleValue());
                dr.createCell(5).setCellValue(a.getKind().name());
                dr.createCell(6).setCellValue(a.getMemo() != null ? a.getMemo() : "");
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTransfers(String yearMonth) throws IOException {
        List<CostAllocation> rows;
        if (yearMonth != null && !yearMonth.isBlank()) {
            rows = allocationRepo.findByYearMonthAndKind(yearMonth, CostAllocation.AllocationKind.TRANSFER);
        } else {
            rows = allocationRepo.findAll().stream()
                .filter(a -> a.getKind() == CostAllocation.AllocationKind.TRANSFER)
                .collect(java.util.stream.Collectors.toList());
        }
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("내부대체");
            CellStyle header = headerStyle(wb);
            String[] cols = {"회계월", "제공본부코드", "수혜본부코드", "공수", "시간당단가", "메모"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (CostAllocation a : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(a.getYearMonth());
                dr.createCell(1).setCellValue(a.getSourceDepartment() != null ? a.getSourceDepartment().getCode() : "");
                dr.createCell(2).setCellValue(a.getTargetDepartment() != null ? a.getTargetDepartment().getCode() : "");
                // Reverse-compute hours and rate from amount is impossible, so store amount as "공수" placeholder
                // Actually for transfers we stored amount = hours * rate, but we don't have hours/rate fields
                // We'll export amount and 1 as rate so reimport works: amount = amount * 1
                dr.createCell(3).setCellValue(a.getAmount().doubleValue());
                dr.createCell(4).setCellValue(1);
                dr.createCell(5).setCellValue(a.getMemo() != null ? a.getMemo() : "");
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportCostItems(String yearMonth) throws IOException {
        List<CostItem> rows;
        if (yearMonth != null && !yearMonth.isBlank()) {
            rows = costItemRepo.findByYearMonth(yearMonth);
        } else {
            rows = costItemRepo.findAll();
        }
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("간접비");
            CellStyle header = headerStyle(wb);
            // Matches import format: 회계월 | 유형 | 본부코드 | 항목 | 금액
            String[] cols = {"회계월", "유형", "본부코드", "항목", "금액"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (CostItem item : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(item.getYearMonth());
                dr.createCell(1).setCellValue(item.getType().name());
                dr.createCell(2).setCellValue(item.getDepartment() != null ? item.getDepartment().getCode() : "");
                dr.createCell(3).setCellValue(item.getCategory());
                dr.createCell(4).setCellValue(item.getAmount().doubleValue());
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportAuditLogs() throws IOException {
        List<AuditLog> rows = auditLogRepo.findTop100ByOrderByCreatedAtDesc();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("감사로그");
            CellStyle header = headerStyle(wb);
            String[] cols = {"시각", "사용자", "액션", "대상", "대상ID", "상세"};
            Row h = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(header);
            }
            int r = 1;
            for (AuditLog a : rows) {
                Row dr = sh.createRow(r++);
                dr.createCell(0).setCellValue(a.getCreatedAt() != null ? a.getCreatedAt().toString().replace("T", " ") : "");
                dr.createCell(1).setCellValue(a.getActor() != null ? a.getActor() : "");
                dr.createCell(2).setCellValue(a.getAction() != null ? a.getAction() : "");
                dr.createCell(3).setCellValue(a.getEntity() != null ? a.getEntity() : "");
                dr.createCell(4).setCellValue(a.getEntityId() != null ? a.getEntityId() : 0);
                dr.createCell(5).setCellValue(a.getDetail() != null ? a.getDetail() : "");
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
