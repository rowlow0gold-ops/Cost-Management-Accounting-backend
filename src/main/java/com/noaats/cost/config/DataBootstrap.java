package com.noaats.cost.config;

import com.noaats.cost.domain.*;
import com.noaats.cost.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class DataBootstrap implements CommandLineRunner {

    private final UserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectRepository projectRepo;
    private final TimesheetRepository timesheetRepo;
    private final PasswordEncoder encoder;
    private final StandardRateRepository rateRepo;

    @Override
    public void run(String... args) {
        if (deptRepo.count() == 0) seedDepartments();
        if (employeeRepo.count() == 0) seedBaseEmployees();
        if (projectRepo.count() == 0) seedBaseProjects();
        if (rateRepo.count() == 0) seedStandardRates();
        if (userRepo.count() == 0) seedUsers();
        if (employeeRepo.count() < 110) seedMoreEmployees();
        if (projectRepo.count() < 120) seedMoreProjects();
        randomizeProjectPeriods();
        seedOverBudgetExample();
        seedSampleTimesheets();
    }

    private void seedUsers() {
        String hash = encoder.encode("password123");
        userRepo.save(User.builder()
            .email("admin@noaats.com").password(hash).name("관리자")
            .role(Role.ADMIN).department(deptRepo.findById(1L).orElse(null)).build());
        userRepo.save(User.builder()
            .email("manager@noaats.com").password(hash).name("본부장")
            .role(Role.MANAGER).department(deptRepo.findById(2L).orElse(null)).build());
        userRepo.save(User.builder()
            .email("user@noaats.com").password(hash).name("일반사용자")
            .role(Role.USER).department(deptRepo.findById(3L).orElse(null)).build());
    }

    private void seedMoreEmployees() {
        // Add employees 51 ~ 110 (12 per department, 5 depts => 60 extras).
        String[] grades = {"사원","사원","대리","대리","과장","과장","과장","차장","차장","부장","부장","부장"};
        BigDecimal[] rates = {
            new BigDecimal("25000"), new BigDecimal("25000"),
            new BigDecimal("35000"), new BigDecimal("35000"),
            new BigDecimal("50000"), new BigDecimal("50000"), new BigDecimal("50000"),
            new BigDecimal("65000"), new BigDecimal("65000"),
            new BigDecimal("85000"), new BigDecimal("85000"), new BigDecimal("85000")
        };
        int seq = 51;
        for (long deptId = 1; deptId <= 5; deptId++) {
            Department d = deptRepo.findById(deptId).orElse(null);
            if (d == null) continue;
            for (int i = 0; i < 12; i++) {
                String empNo = String.format("E%04d", seq);
                if (employeeRepo.findAll().stream().anyMatch(e -> e.getEmpNo().equals(empNo))) {
                    seq++; continue;
                }
                employeeRepo.save(Employee.builder()
                    .empNo(empNo)
                    .name("추가직원" + seq)
                    .grade(grades[i])
                    .department(d)
                    .hourlyRate(rates[i])
                    .build());
                seq++;
            }
        }
    }

    private void seedMoreProjects() {
        // Add projects 21 ~ 120 (20 existing + 100 extras).
        for (int i = 21; i <= 120; i++) {
            String code = String.format("PRJ-%03d", i);
            if (projectRepo.findAll().stream().anyMatch(p -> p.getCode().equals(code))) continue;
            long deptId = ((i - 1) % 5) + 1;
            Department d = deptRepo.findById(deptId).orElse(null);
            if (d == null) continue;
            projectRepo.save(Project.builder()
                .code(code)
                .name("추가 프로젝트 " + i)
                .ownerDepartment(d)
                .budgetHours(BigDecimal.valueOf(1000L + (i * 10L)))
                .budgetCost(BigDecimal.valueOf(60_000_000L + (i * 500_000L)))
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build());
        }
    }

    /**
     * Give each project a different start date and duration.
     * Deterministic (seeded Random) so the demo is stable across restarts.
     * Only runs once — checks if every project still has the default span.
     */
    private void randomizeProjectPeriods() {
        LocalDate defaultStart = LocalDate.of(2025, 1, 1);
        LocalDate defaultEnd = LocalDate.of(2026, 12, 31);
        boolean allDefault = projectRepo.findAll().stream().allMatch(p ->
            defaultStart.equals(p.getStartDate()) && defaultEnd.equals(p.getEndDate()));
        if (!allDefault) return; // already randomized before

        Random r = new Random(42);
        for (Project p : projectRepo.findAll()) {
            int startYear = 2024 + r.nextInt(2);          // 2024 or 2025
            int startMonth = 1 + r.nextInt(12);           // 1..12
            int durationMonths = 6 + r.nextInt(19);       // 6..24
            LocalDate start = LocalDate.of(startYear, startMonth, 1);
            LocalDate end = start.plusMonths(durationMonths).minusDays(1);
            p.setStartDate(start);
            p.setEndDate(end);
            projectRepo.save(p);
        }
    }

    /**
     * Create an over-budget scenario so "예산초과 프로젝트" KPI shows > 0.
     * We pick PRJ-004 (조직혁신 프로젝트, annual budget 70M) and dump 400h of
     * 부장-level work in 2026-01 → ~34M actual, while the Q1 phased monthly
     * budget is only ~4.67M. Result: massive over-budget variance.
     */
    private void seedOverBudgetExample() {
        Project overBudget = projectRepo.findAll().stream()
            .filter(p -> "PRJ-004".equals(p.getCode()))
            .findFirst().orElse(null);
        if (overBudget == null) return;

        // If we already injected this demo, skip.
        boolean already = timesheetRepo.findAll().stream()
            .anyMatch(t -> t.getProject().getId().equals(overBudget.getId())
                        && t.getMemo() != null
                        && t.getMemo().contains("[DEMO: 예산초과]"));
        if (already) return;

        // Find a 부장-level employee in any department.
        Employee senior = employeeRepo.findAll().stream()
            .filter(e -> "부장".equals(e.getGrade()))
            .findFirst().orElse(null);
        if (senior == null) return;

        // Spread 400h across 50 work days (8h each) in Jan 2026
        int remaining = 400;
        int day = 2;
        while (remaining > 0 && day <= 28) {
            int h = Math.min(remaining, 8);
            LocalDate date = LocalDate.of(2026, 1, day);
            timesheetRepo.save(Timesheet.builder()
                .employee(senior)
                .project(overBudget)
                .workDate(date)
                .hours(BigDecimal.valueOf(h))
                .memo("[DEMO: 예산초과] 긴급 투입")
                .status(Timesheet.Status.APPROVED)
                .submittedAt(date.atTime(18, 0))
                .approvedAt(date.plusDays(1).atTime(9, 0))
                .approvedByEmail("manager@noaats.com")
                .build());
            remaining -= h;
            day++;
        }
    }

    // ── Base seed methods (run on any profile, including prod) ──────────

    private void seedDepartments() {
        String[][] depts = {
            {"HQ1", "경영기획본부"}, {"HQ2", "자산운용본부"}, {"HQ3", "IT본부"},
            {"HQ4", "리스크관리본부"}, {"HQ5", "컴플라이언스본부"},
        };
        for (String[] d : depts) {
            deptRepo.save(Department.builder().code(d[0]).name(d[1]).build());
        }
    }

    private void seedBaseEmployees() {
        Object[][] emps = {
            // {empNo, name, grade, deptIdx(1-based), hourlyRate}
            {"E0001","김민수","사원",1,25000}, {"E0002","이영희","사원",1,25000},
            {"E0003","박지훈","대리",1,35000}, {"E0004","최서연","대리",1,35000},
            {"E0005","정우진","대리",1,35000}, {"E0006","강하늘","과장",1,50000},
            {"E0007","조민재","과장",1,50000}, {"E0008","윤서아","차장",1,65000},
            {"E0009","임도현","차장",1,65000}, {"E0010","한지민","부장",1,85000},

            {"E0011","오은서","사원",2,25000}, {"E0012","신유진","사원",2,25000},
            {"E0013","홍기범","대리",2,35000}, {"E0014","송채원","대리",2,35000},
            {"E0015","문수아","대리",2,35000}, {"E0016","노태현","과장",2,50000},
            {"E0017","구민호","과장",2,50000}, {"E0018","유나래","차장",2,65000},
            {"E0019","곽재훈","차장",2,65000}, {"E0020","백승호","부장",2,85000},

            {"E0021","권다은","사원",3,25000}, {"E0022","전소희","사원",3,25000},
            {"E0023","류준영","대리",3,35000}, {"E0024","서가영","대리",3,35000},
            {"E0025","남궁현","대리",3,35000}, {"E0026","채윤석","과장",3,50000},
            {"E0027","지은별","과장",3,50000}, {"E0028","우상현","차장",3,65000},
            {"E0029","안세빈","차장",3,65000}, {"E0030","배준영","부장",3,85000},

            {"E0031","심예린","사원",4,25000}, {"E0032","전미라","사원",4,25000},
            {"E0033","석호준","대리",4,35000}, {"E0034","민가현","대리",4,35000},
            {"E0035","도재민","대리",4,35000}, {"E0036","함은수","과장",4,50000},
            {"E0037","진세호","과장",4,50000}, {"E0038","오리아","차장",4,65000},
            {"E0039","감지효","차장",4,65000}, {"E0040","피영준","부장",4,85000},

            {"E0041","진수아","사원",5,25000}, {"E0042","임혁","사원",5,25000},
            {"E0043","반지현","대리",5,35000}, {"E0044","노정훈","대리",5,35000},
            {"E0045","유선아","대리",5,35000}, {"E0046","금하늘","과장",5,50000},
            {"E0047","단우현","과장",5,50000}, {"E0048","갈예진","차장",5,65000},
            {"E0049","맹승현","차장",5,65000}, {"E0050","우경한","부장",5,85000},
        };
        for (Object[] e : emps) {
            Department d = deptRepo.findById(((Number) e[3]).longValue()).orElse(null);
            if (d == null) continue;
            employeeRepo.save(Employee.builder()
                .empNo((String) e[0]).name((String) e[1]).grade((String) e[2])
                .department(d).hourlyRate(BigDecimal.valueOf(((Number) e[4]).longValue()))
                .build());
        }
    }

    private void seedBaseProjects() {
        Object[][] projs = {
            {"PRJ-001","경영전략 수립",1,1600,100_000_000},
            {"PRJ-002","중장기 사업계획",1,1400,90_000_000},
            {"PRJ-003","예산편성 시스템",1,1200,80_000_000},
            {"PRJ-004","조직혁신 프로젝트",1,1000,70_000_000},
            {"PRJ-005","자산배분 모델링",2,1800,120_000_000},
            {"PRJ-006","대체투자 운용",2,1600,110_000_000},
            {"PRJ-007","글로벌 펀드 운용",2,1500,105_000_000},
            {"PRJ-008","채권 포트폴리오 최적화",2,1400,95_000_000},
            {"PRJ-009","코어뱅킹 차세대",3,2400,180_000_000},
            {"PRJ-010","클라우드 마이그레이션",3,2000,150_000_000},
            {"PRJ-011","데이터 플랫폼 구축",3,1800,140_000_000},
            {"PRJ-012","보안관제 고도화",3,1500,110_000_000},
            {"PRJ-013","VaR 모델 개선",4,1400,95_000_000},
            {"PRJ-014","신용리스크 관리",4,1300,90_000_000},
            {"PRJ-015","시장리스크 한도관리",4,1200,85_000_000},
            {"PRJ-016","스트레스 테스트",4,1100,80_000_000},
            {"PRJ-017","내부통제 점검",5,1200,85_000_000},
            {"PRJ-018","자금세탁방지 (AML)",5,1400,100_000_000},
            {"PRJ-019","법규 변경 대응",5,1100,80_000_000},
            {"PRJ-020","컴플라이언스 교육",5,900,60_000_000},
        };
        for (Object[] p : projs) {
            Department d = deptRepo.findById(((Number) p[2]).longValue()).orElse(null);
            if (d == null) continue;
            projectRepo.save(Project.builder()
                .code((String) p[0]).name((String) p[1]).ownerDepartment(d)
                .budgetHours(BigDecimal.valueOf(((Number) p[3]).longValue()))
                .budgetCost(BigDecimal.valueOf(((Number) p[4]).longValue()))
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build());
        }
    }

    private void seedStandardRates() {
        String[][] months = {
            {"2026-04","25000","35000","50000","65000","85000"},
            {"2026-03","25000","35000","50000","65000","85000"},
            {"2026-02","25000","35000","50000","65000","85000"},
            {"2026-01","24500","34000","49000","64000","83000"},
            {"2025-12","24000","33000","48000","63000","82000"},
        };
        String[] grades = {"사원","대리","과장","차장","부장"};
        for (String[] m : months) {
            for (int i = 0; i < grades.length; i++) {
                rateRepo.save(StandardRate.builder()
                    .yearMonth(m[0]).grade(grades[i])
                    .hourlyRate(new BigDecimal(m[i + 1]))
                    .build());
            }
        }
    }

    /**
     * Seed sample timesheets across multiple months so the dashboard has data.
     * Each entry is a single workday with hours ≤ 8 (realistic daily timesheet).
     * To get monthly totals, we spread hours across multiple days.
     * Skips if any APPROVED timesheets already exist (idempotent).
     */
    private void seedSampleTimesheets() {
        boolean hasApproved = timesheetRepo.findAll().stream()
            .anyMatch(t -> t.getStatus() == Timesheet.Status.APPROVED
                        && t.getMemo() != null && !t.getMemo().contains("[DEMO: 예산초과]"));
        if (hasApproved) return;

        // April 2026 — spread across work days (each row: empId, projId, totalHours, memo)
        int[][] april = {
            {1,1,80},{3,2,75},{6,3,60},{8,4,50},
            {11,5,90},{13,6,80},{16,7,70},{18,8,65},
            {21,9,120},{23,10,100},{26,11,95},{28,12,75},
            {31,13,70},{33,14,65},{36,15,60},{38,16,55},
            {41,17,60},{43,18,70},{46,19,55},{48,20,45},
        };
        for (int[] r : april) {
            spreadApproved(r[0], r[1], 2026, 4, r[2], "4월 공수");
        }

        // March 2026
        int[][] march = {{1,1,60},{11,5,70},{21,9,110},{31,13,65},{41,17,55}};
        for (int[] r : march) spreadApproved(r[0], r[1], 2026, 3, r[2], "3월 공수");

        // Feb 2026
        int[][] feb = {{3,2,55},{13,6,75},{23,10,95},{33,14,60}};
        for (int[] r : feb) spreadApproved(r[0], r[1], 2026, 2, r[2], "2월 공수");

        // Jan 2026
        int[][] jan = {{6,3,50},{16,7,65},{26,11,85}};
        for (int[] r : jan) spreadApproved(r[0], r[1], 2026, 1, r[2], "1월 공수");

        // Dec 2025
        int[][] dec = {
            {1,1,40},{3,2,45},{8,4,40},{11,5,85},{13,6,70},{18,8,60},
            {21,9,110},{23,10,95},{28,12,70},{31,13,60},{36,15,50},{41,17,55},{46,19,50},
        };
        for (int[] r : dec) spreadApproved(r[0], r[1], 2025, 12, r[2], "12월 공수");

        // A few SUBMITTED and DRAFT entries
        saveSubmitted(2, 1, LocalDate.of(2026, 4, 19), 6, "리서치 보조");
        saveSubmitted(4, 2, LocalDate.of(2026, 4, 19), 8, "문서 정리");
        saveSubmitted(12, 5, LocalDate.of(2026, 4, 19), 7, "시장 분석");
        saveDraft(22, 9, LocalDate.of(2026, 4, 20), 8, "설계 검토");
    }

    /**
     * Spread totalHours across multiple work days in the given month.
     * Each day gets up to 8 hours.
     */
    private void spreadApproved(long empId, long projId, int year, int month, int totalHours, String memo) {
        Employee emp = employeeRepo.findById(empId).orElse(null);
        Project proj = projectRepo.findById(projId).orElse(null);
        if (emp == null || proj == null) return;

        int remaining = totalHours;
        int day = 2; // start from 2nd of the month
        while (remaining > 0) {
            int h = Math.min(remaining, 8);
            LocalDate date = LocalDate.of(year, month, day);
            timesheetRepo.save(Timesheet.builder()
                .employee(emp).project(proj).workDate(date)
                .hours(BigDecimal.valueOf(h)).memo(memo)
                .status(Timesheet.Status.APPROVED)
                .submittedAt(date.atTime(18, 0))
                .approvedAt(date.plusDays(1).atTime(10, 0))
                .approvedByEmail("manager@noaats.com")
                .build());
            remaining -= h;
            day++;
            if (day > 28) break; // safety: don't exceed month
        }
    }

    private void saveSubmitted(long empId, long projId, LocalDate date, int hours, String memo) {
        Employee emp = employeeRepo.findById(empId).orElse(null);
        Project proj = projectRepo.findById(projId).orElse(null);
        if (emp == null || proj == null) return;
        timesheetRepo.save(Timesheet.builder()
            .employee(emp).project(proj).workDate(date)
            .hours(BigDecimal.valueOf(hours)).memo(memo)
            .status(Timesheet.Status.SUBMITTED)
            .submittedAt(date.atTime(18, 0))
            .build());
    }

    private void saveDraft(long empId, long projId, LocalDate date, int hours, String memo) {
        Employee emp = employeeRepo.findById(empId).orElse(null);
        Project proj = projectRepo.findById(projId).orElse(null);
        if (emp == null || proj == null) return;
        timesheetRepo.save(Timesheet.builder()
            .employee(emp).project(proj).workDate(date)
            .hours(BigDecimal.valueOf(hours)).memo(memo)
            .status(Timesheet.Status.DRAFT)
            .build());
    }
}
