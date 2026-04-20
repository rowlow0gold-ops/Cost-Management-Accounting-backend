package com.noaats.cost.service;

import com.noaats.cost.dto.CostDtos.CostAggregateRow;
import com.noaats.cost.dto.CostDtos.VarianceRow;
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
