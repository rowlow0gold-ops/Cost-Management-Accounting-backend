package com.noaats.cost.service;

import com.noaats.cost.domain.CostAllocation;
import com.noaats.cost.domain.Department;
import com.noaats.cost.repository.CostAllocationRepository;
import com.noaats.cost.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferImportService {

    private final CostAllocationRepository allocationRepo;
    private final DepartmentRepository deptRepo;
    private final AuditService audit;

    private static final int MAX_IMPORT_ROWS = 200;
    private static final String[] EXPECTED_HEADERS = {"회계월", "제공본부코드", "수혜본부코드", "공수", "시간당단가", "메모"};

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
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx"))
            throw new IllegalArgumentException(".xlsx 파일만 업로드 가능합니다.");

        Workbook wb;
        try { wb = new XSSFWorkbook(file.getInputStream()); }
        catch (Exception e) { throw new IllegalArgumentException("파일을 읽을 수 없습니다."); }

        try (wb) {
            if (wb.getNumberOfSheets() == 0) throw new IllegalArgumentException("시트가 없습니다.");
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IllegalArgumentException("헤더 행이 없습니다.");
            validateHeaders(headerRow);

            int dataRows = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r != null && getCellString(r.getCell(0)).length() > 0) dataRows++;
            }
            if (dataRows == 0) throw new IllegalArgumentException("데이터가 없습니다.");
            if (dataRows > MAX_IMPORT_ROWS) throw new IllegalArgumentException("최대 " + MAX_IMPORT_ROWS + "건까지 업로드 가능합니다. (현재 " + dataRows + "건)");

            Map<String, Department> deptMap = deptRepo.findAll().stream()
                .collect(Collectors.toMap(Department::getCode, d -> d));

            List<String> errors = new ArrayList<>();
            int valid = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String ym = getCellString(row.getCell(0));
                    String srcCode = getCellString(row.getCell(1));
                    String tgtCode = getCellString(row.getCell(2));
                    BigDecimal hours = getCellDecimal(row.getCell(3));
                    BigDecimal rate = getCellDecimal(row.getCell(4));

                    if (ym.isBlank() && srcCode.isBlank()) continue;
                    if (ym.isBlank() || srcCode.isBlank() || tgtCode.isBlank() || hours == null || rate == null) {
                        errors.add("행 " + (i + 1) + ": 필수값 누락");
                        continue;
                    }
                    if (!ym.matches("\\d{4}-\\d{2}")) {
                        errors.add("행 " + (i + 1) + ": 회계월 형식 오류 (yyyy-MM)");
                        continue;
                    }
                    if (srcCode.equals(tgtCode)) {
                        errors.add("행 " + (i + 1) + ": 제공 본부와 수혜 본부가 같습니다");
                        continue;
                    }
                    if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("행 " + (i + 1) + ": 공수는 0보다 커야 합니다");
                        continue;
                    }
                    if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("행 " + (i + 1) + ": 단가는 0보다 커야 합니다");
                        continue;
                    }
                    if (!deptMap.containsKey(srcCode)) {
                        errors.add("행 " + (i + 1) + ": 제공본부 '" + srcCode + "' 없음");
                        continue;
                    }
                    if (!deptMap.containsKey(tgtCode)) {
                        errors.add("행 " + (i + 1) + ": 수혜본부 '" + tgtCode + "' 없음");
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

            if ("REPLACE".equalsIgnoreCase(mode)) {
                List<CostAllocation> existing = allocationRepo.findByYearMonthAndKind(
                    getCellString(sheet.getRow(1).getCell(0)),
                    CostAllocation.AllocationKind.TRANSFER);
                // Delete all TRANSFER kind entries
                var allTransfers = allocationRepo.findAll().stream()
                    .filter(a -> a.getKind() == CostAllocation.AllocationKind.TRANSFER)
                    .collect(Collectors.toList());
                allocationRepo.deleteAll(allTransfers);
                audit.log("DELETE_TRANSFER_REPLACE", "ALLOCATION", null, "deleted=" + allTransfers.size());
            }

            int imported = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String ym = getCellString(row.getCell(0));
                String srcCode = getCellString(row.getCell(1));
                String tgtCode = getCellString(row.getCell(2));
                BigDecimal hours = getCellDecimal(row.getCell(3));
                BigDecimal rate = getCellDecimal(row.getCell(4));
                String memo = row.getCell(5) != null ? getCellString(row.getCell(5)) : "";

                if (ym.isBlank() && srcCode.isBlank()) continue;

                BigDecimal amount = hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                allocationRepo.save(CostAllocation.builder()
                    .yearMonth(ym)
                    .sourceDepartment(deptMap.get(srcCode))
                    .targetDepartment(deptMap.get(tgtCode))
                    .basis(CostAllocation.AllocationBasis.HOURS)
                    .amount(amount)
                    .kind(CostAllocation.AllocationKind.TRANSFER)
                    .memo(memo)
                    .createdAt(LocalDateTime.now())
                    .build());
                imported++;
            }
            return imported;
        }
    }

    private void validateHeaders(Row headerRow) {
        for (int i = 0; i < 5; i++) {
            Cell cell = headerRow.getCell(i);
            String actual = cell != null ? getCellString(cell) : "";
            if (!EXPECTED_HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException(
                    "헤더 형식이 올바르지 않습니다. " + (i + 1) + "번째 열은 '" + EXPECTED_HEADERS[i] +
                    "'이어야 하지만 '" + actual + "'입니다.\n" +
                    "올바른 헤더: 회계월 | 제공본부코드 | 수혜본부코드 | 공수 | 시간당단가 | 메모");
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
