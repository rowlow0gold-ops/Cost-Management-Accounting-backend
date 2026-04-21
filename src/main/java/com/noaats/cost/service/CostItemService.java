package com.noaats.cost.service;

import com.noaats.cost.domain.CostItem;
import com.noaats.cost.domain.Department;
import com.noaats.cost.repository.CostItemRepository;
import com.noaats.cost.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostItemService {

    private final CostItemRepository costItemRepo;
    private final DepartmentRepository deptRepo;
    private final AuditService audit;

    private static final int MAX_IMPORT_ROWS = 200;
    private static final String[] EXPECTED_HEADERS = {"회계월", "유형", "본부코드", "항목", "금액"};

    /**
     * Validate only — parse the file and return the count of valid rows.
     * Throws on any error.
     */
    public int validateExcel(MultipartFile file) throws IOException {
        return processExcel(file, "MERGE", true);
    }

    @Transactional
    public int importFromExcel(MultipartFile file, String mode) throws IOException {
        return processExcel(file, mode, false);
    }

    private int processExcel(MultipartFile file, String mode, boolean dryRun) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("파일이 비어 있습니다.");

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. .xlsx 파일만 업로드 가능합니다.");
        }

        Workbook wb;
        try {
            wb = new XSSFWorkbook(file.getInputStream());
        } catch (Exception e) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다. 올바른 .xlsx 파일인지 확인하세요.");
        }

        try (wb) {
            if (wb.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("시트가 없습니다.");
            }
            Sheet sheet = wb.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("헤더 행이 없습니다. 첫 행에 '회계월, 유형, 본부코드, 항목, 금액' 컬럼이 필요합니다.");
            }
            validateHeaders(headerRow);

            int dataRows = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r != null && getCellString(r.getCell(0)).length() > 0) dataRows++;
            }
            if (dataRows == 0) {
                throw new IllegalArgumentException("데이터가 없습니다. 2행부터 데이터를 입력하세요.");
            }
            if (dataRows > MAX_IMPORT_ROWS) {
                throw new IllegalArgumentException("최대 " + MAX_IMPORT_ROWS + "건까지 업로드 가능합니다. (현재 " + dataRows + "건)");
            }

            Map<String, Department> deptMap = deptRepo.findAll().stream()
                .collect(Collectors.toMap(Department::getCode, d -> d));

            List<String> errors = new ArrayList<>();
            int valid = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String yearMonth = getCellString(row.getCell(0));
                    String typeStr = getCellString(row.getCell(1));
                    String deptCode = getCellString(row.getCell(2));
                    String category = getCellString(row.getCell(3));
                    BigDecimal amount = getCellDecimal(row.getCell(4));

                    if (yearMonth.isBlank() && typeStr.isBlank()) continue;

                    if (yearMonth.isBlank() || typeStr.isBlank() || deptCode.isBlank()
                            || category.isBlank() || amount == null) {
                        errors.add("행 " + (i + 1) + ": 필수값 누락 (회계월, 유형, 본부코드, 항목, 금액은 필수)");
                        continue;
                    }

                    if (!yearMonth.matches("\\d{4}-\\d{2}")) {
                        errors.add("행 " + (i + 1) + ": 회계월 형식 오류 (yyyy-MM, 예: 2026-04)");
                        continue;
                    }

                    CostItem.CostType type;
                    try {
                        type = CostItem.CostType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errors.add("행 " + (i + 1) + ": 유형은 DIRECT 또는 INDIRECT만 가능합니다 (현재: " + typeStr + ")");
                        continue;
                    }

                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("행 " + (i + 1) + ": 금액은 0보다 커야 합니다");
                        continue;
                    }

                    Department dept = deptMap.get(deptCode);
                    if (dept == null) {
                        errors.add("행 " + (i + 1) + ": 본부코드 '" + deptCode + "' 없음");
                        continue;
                    }

                    valid++;
                } catch (Exception e) {
                    errors.add("행 " + (i + 1) + ": " + e.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(
                    (valid == 0 ? "전체 실패" : "일부 오류") +
                    " (" + errors.size() + "건 오류):\n" +
                    String.join("\n", errors.subList(0, Math.min(errors.size(), 10))));
            }

            if (dryRun) return valid;

            // --- actual import below ---

            // REPLACE mode: delete ALL existing cost items
            if ("REPLACE".equalsIgnoreCase(mode)) {
                long existingCount = costItemRepo.count();
                if (existingCount > 0) {
                    costItemRepo.deleteAll();
                    audit.log("DELETE_COSTITEM_REPLACE", "COST_ITEM", null,
                        "deleted=" + existingCount + " (all)");
                }
            }

            int imported = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String yearMonth = getCellString(row.getCell(0));
                String typeStr = getCellString(row.getCell(1));
                String deptCode = getCellString(row.getCell(2));
                String category = getCellString(row.getCell(3));
                BigDecimal amount = getCellDecimal(row.getCell(4));

                if (yearMonth.isBlank() && typeStr.isBlank()) continue;

                CostItem item = CostItem.builder()
                    .yearMonth(yearMonth)
                    .type(CostItem.CostType.valueOf(typeStr.toUpperCase()))
                    .department(deptMap.get(deptCode))
                    .category(category)
                    .amount(amount)
                    .build();
                costItemRepo.save(item);
                imported++;
            }

            return imported;
        }
    }

    private void validateHeaders(Row headerRow) {
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            Cell cell = headerRow.getCell(i);
            String actual = cell != null ? getCellString(cell) : "";
            if (!EXPECTED_HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException(
                    "헤더 형식이 올바르지 않습니다. " +
                    (i + 1) + "번째 열은 '" + EXPECTED_HEADERS[i] + "'이어야 하지만 '" + actual + "'입니다.\n" +
                    "올바른 헤더: 회계월 | 유형 | 본부코드 | 항목 | 금액");
            }
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return cell.getStringCellValue().trim();
    }

    private BigDecimal getCellDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        String s = getCellString(cell);
        if (s.isBlank()) return null;
        return new BigDecimal(s);
    }
}
