package com.noaats.cost.service;

import com.noaats.cost.domain.Employee;
import com.noaats.cost.domain.Project;
import com.noaats.cost.domain.Timesheet;
import com.noaats.cost.dto.CostDtos.TimesheetRequest;
import com.noaats.cost.repository.EmployeeRepository;
import com.noaats.cost.repository.ProjectRepository;
import com.noaats.cost.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectRepository projectRepo;
    private final AuditService audit;

    @Transactional
    public Timesheet create(TimesheetRequest req) {
        // --- server-side validation ---
        if (req.getEmployeeId() == null) throw new IllegalArgumentException("직원을 선택하세요");
        if (req.getProjectId()  == null) throw new IllegalArgumentException("프로젝트를 선택하세요");
        if (req.getWorkDate()   == null) throw new IllegalArgumentException("근무일을 입력하세요");
        if (req.getWorkDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("미래 날짜는 입력할 수 없습니다");
        if (req.getHours() == null || req.getHours().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("공수는 0보다 커야 합니다");
        if (req.getHours().compareTo(BigDecimal.valueOf(24)) > 0)
            throw new IllegalArgumentException("하루 최대 24시간까지 입력 가능합니다");

        Timesheet t = Timesheet.builder()
            .employee(employeeRepo.findById(req.getEmployeeId()).orElseThrow())
            .project(projectRepo.findById(req.getProjectId()).orElseThrow())
            .workDate(req.getWorkDate())
            .hours(req.getHours())
            .memo(req.getMemo())
            .status(Timesheet.Status.DRAFT)
            .build();
        Timesheet saved = timesheetRepo.save(t);
        audit.log("CREATE_TIMESHEET", "TIMESHEET", saved.getId(),
                  "emp=" + saved.getEmployee().getEmpNo() + " proj=" + saved.getProject().getCode() +
                  " hours=" + saved.getHours());
        return saved;
    }

    @Transactional
    public Timesheet submit(Long id) {
        Timesheet t = timesheetRepo.findById(id).orElseThrow();
        if (t.getStatus() != Timesheet.Status.DRAFT)
            throw new IllegalStateException("Only DRAFT can be submitted");
        t.setStatus(Timesheet.Status.SUBMITTED);
        t.setSubmittedAt(LocalDateTime.now());
        audit.log("SUBMIT_TIMESHEET", "TIMESHEET", id, null);
        return t;
    }

    @Transactional
    public Timesheet approve(Long id, String approverEmail) {
        Timesheet t = timesheetRepo.findById(id).orElseThrow();
        if (t.getStatus() != Timesheet.Status.SUBMITTED)
            throw new IllegalStateException("Only SUBMITTED can be approved");
        t.setStatus(Timesheet.Status.APPROVED);
        t.setApprovedAt(LocalDateTime.now());
        t.setApprovedByEmail(approverEmail);
        audit.log("APPROVE_TIMESHEET", "TIMESHEET", id, "by=" + approverEmail);
        return t;
    }

    @Transactional
    public Timesheet reject(Long id, String approverEmail) {
        Timesheet t = timesheetRepo.findById(id).orElseThrow();
        if (t.getStatus() != Timesheet.Status.SUBMITTED)
            throw new IllegalStateException("Only SUBMITTED can be rejected");
        t.setStatus(Timesheet.Status.REJECTED);
        t.setApprovedByEmail(approverEmail);
        audit.log("REJECT_TIMESHEET", "TIMESHEET", id, "by=" + approverEmail);
        return t;
    }

    @Transactional
    public void delete(Long id) {
        timesheetRepo.deleteById(id);
        audit.log("DELETE_TIMESHEET", "TIMESHEET", id, null);
    }

    // ---- Bulk actions ----

    @Transactional
    public int bulkSubmit() {
        List<Timesheet> drafts = timesheetRepo.findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status.DRAFT);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Timesheet t : drafts) {
            t.setStatus(Timesheet.Status.SUBMITTED);
            t.setSubmittedAt(now);
        }
        audit.log("BULK_SUBMIT_TIMESHEET", "TIMESHEET", null, "count=" + drafts.size());
        return drafts.size();
    }

    @Transactional
    public int bulkApprove(String approverEmail) {
        List<Timesheet> submitted = timesheetRepo.findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status.SUBMITTED);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Timesheet t : submitted) {
            t.setStatus(Timesheet.Status.APPROVED);
            t.setApprovedAt(now);
            t.setApprovedByEmail(approverEmail);
        }
        audit.log("BULK_APPROVE_TIMESHEET", "TIMESHEET", null, "count=" + submitted.size() + " by=" + approverEmail);
        return submitted.size();
    }

    @Transactional
    public int bulkReject(String approverEmail) {
        List<Timesheet> submitted = timesheetRepo.findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status.SUBMITTED);
        for (Timesheet t : submitted) {
            t.setStatus(Timesheet.Status.REJECTED);
            t.setApprovedByEmail(approverEmail);
        }
        audit.log("BULK_REJECT_TIMESHEET", "TIMESHEET", null, "count=" + submitted.size() + " by=" + approverEmail);
        return submitted.size();
    }

    @Transactional
    public int bulkDeleteDraft() {
        List<Timesheet> drafts = timesheetRepo.findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status.DRAFT);
        timesheetRepo.deleteAll(drafts);
        audit.log("BULK_DELETE_TIMESHEET", "TIMESHEET", null, "count=" + drafts.size());
        return drafts.size();
    }

    /**
     * Import timesheets from an Excel file (.xlsx).
     * Expected columns: 사번(empNo) | 프로젝트코드(projectCode) | 근무일(workDate) | 시간(hours) | 메모(memo)
     */
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final String[] EXPECTED_HEADERS = {"사번", "프로젝트코드", "근무일", "시간", "메모", "Action"};

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
                throw new IllegalArgumentException("헤더 행이 없습니다. 첫 행에 '사번, 프로젝트코드, 근무일, 시간, 메모' 컬럼이 필요합니다.");
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

            Map<String, Employee> empMap = employeeRepo.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmpNo, e -> e));
            Map<String, Project> projMap = projectRepo.findAll().stream()
                .collect(Collectors.toMap(Project::getCode, p -> p));

            List<String> errors = new ArrayList<>();
            int valid = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String empNo = getCellString(row.getCell(0));
                    String projCode = getCellString(row.getCell(1));
                    LocalDate workDate = getCellDate(row.getCell(2));
                    BigDecimal hours = getCellDecimal(row.getCell(3));

                    if (empNo.isBlank() && projCode.isBlank()) continue;

                    if (empNo.isBlank() || projCode.isBlank() || workDate == null || hours == null) {
                        errors.add("행 " + (i + 1) + ": 필수값 누락 (사번, 프로젝트코드, 근무일, 시간은 필수)");
                        continue;
                    }

                    if (hours.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(BigDecimal.valueOf(24)) > 0) {
                        errors.add("행 " + (i + 1) + ": 시간은 0~24 사이여야 합니다 (현재: " + hours + ")");
                        continue;
                    }

                    if (workDate.isAfter(LocalDate.now())) {
                        errors.add("행 " + (i + 1) + ": 미래 날짜는 입력할 수 없습니다 (" + workDate + ")");
                        continue;
                    }

                    Employee emp = empMap.get(empNo);
                    if (emp == null) { errors.add("행 " + (i + 1) + ": 사번 '" + empNo + "' 없음"); continue; }

                    Project proj = projMap.get(projCode);
                    if (proj == null) { errors.add("행 " + (i + 1) + ": 프로젝트 '" + projCode + "' 없음"); continue; }

                    // Validate Action column (optional — defaults to DRAFT)
                    String action = row.getCell(5) != null ? getCellString(row.getCell(5)).toUpperCase() : "";
                    if (!action.isEmpty() && !"DRAFT".equals(action) && !"SUBMITTED".equals(action)
                            && !"APPROVED".equals(action) && !"REJECTED".equals(action)) {
                        errors.add("행 " + (i + 1) + ": Action은 DRAFT/SUBMITTED/APPROVED/REJECTED 중 하나여야 합니다 (현재: " + action + ")");
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

            // REPLACE mode: delete ALL existing timesheets
            if ("REPLACE".equalsIgnoreCase(mode)) {
                long existingCount = timesheetRepo.count();
                if (existingCount > 0) {
                    timesheetRepo.deleteAll();
                    audit.log("DELETE_TIMESHEET_REPLACE", "TIMESHEET", null,
                        "deleted=" + existingCount + " (all)");
                }
            }

            int imported = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String empNo = getCellString(row.getCell(0));
                String projCode = getCellString(row.getCell(1));
                LocalDate workDate = getCellDate(row.getCell(2));
                BigDecimal hours = getCellDecimal(row.getCell(3));
                String memo = row.getCell(4) != null ? getCellString(row.getCell(4)) : "";
                String action = row.getCell(5) != null ? getCellString(row.getCell(5)).toUpperCase() : "";

                if (empNo.isBlank() && projCode.isBlank()) continue;

                Timesheet.Status status = Timesheet.Status.DRAFT;
                if (!action.isEmpty()) {
                    status = Timesheet.Status.valueOf(action);
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime submittedAt = null;
                LocalDateTime approvedAt = null;
                if (status == Timesheet.Status.SUBMITTED || status == Timesheet.Status.APPROVED || status == Timesheet.Status.REJECTED) {
                    submittedAt = now;
                }
                if (status == Timesheet.Status.APPROVED || status == Timesheet.Status.REJECTED) {
                    approvedAt = now;
                }

                Timesheet t = Timesheet.builder()
                    .employee(empMap.get(empNo)).project(projMap.get(projCode))
                    .workDate(workDate).hours(hours).memo(memo)
                    .status(status).submittedAt(submittedAt).approvedAt(approvedAt)
                    .build();
                timesheetRepo.save(t);
                imported++;
            }

            return imported;
        }
    }

    private void validateHeaders(Row headerRow) {
        // First 5 headers are required, 6th (Action) is optional
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            Cell cell = headerRow.getCell(i);
            String actual = cell != null ? getCellString(cell) : "";
            if (i < 5 && !EXPECTED_HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException(
                    "헤더 형식이 올바르지 않습니다. " +
                    (i + 1) + "번째 열은 '" + EXPECTED_HEADERS[i] + "'이어야 하지만 '" + actual + "'입니다.\n" +
                    "올바른 헤더: 사번 | 프로젝트코드 | 근무일 | 시간 | 메모 | Action");
            }
            // If Action header exists, validate it matches
            if (i == 5 && !actual.isEmpty() && !EXPECTED_HEADERS[i].equals(actual)) {
                throw new IllegalArgumentException(
                    "헤더 형식이 올바르지 않습니다. 6번째 열은 'Action'이어야 하지만 '" + actual + "'입니다.\n" +
                    "올바른 헤더: 사번 | 프로젝트코드 | 근무일 | 시간 | 메모 | Action");
            }
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return cell.getStringCellValue().trim();
    }

    private LocalDate getCellDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        // Try parsing string date (yyyy-MM-dd)
        String s = getCellString(cell);
        if (s.isBlank()) return null;
        return LocalDate.parse(s);
    }

    private BigDecimal getCellDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        String s = getCellString(cell);
        if (s.isBlank()) return null;
        return new BigDecimal(s);
    }
}
