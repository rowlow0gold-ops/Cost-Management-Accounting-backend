package com.noaats.cost.config;

import com.noaats.cost.domain.*;
import com.noaats.cost.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Realistic demo data for a medium-sized financial company (~150 employees).
 * Covers 12 months (2025-07 → 2026-06) with timesheets, cost items,
 * allocations, transfers, and audit logs.
 *
 * Idempotent: each section checks before inserting.
 */
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
    private final CostAllocationRepository allocationRepo;
    private final CostItemRepository costItemRepo;
    private final AuditLogRepository auditLogRepo;

    private final Random rng = new Random(42); // deterministic

    @Override
    public void run(String... args) {
        migrateAllocationKind();
        if (deptRepo.count() == 0) seedDepartments();
        if (userRepo.count() == 0) seedUsers();
        if (employeeRepo.count() < 140) seedEmployees();
        if (projectRepo.count() < 30) seedProjects();
        if (rateRepo.count() == 0) seedStandardRates();
        if (timesheetRepo.count() < 1000) seedTimesheets();
        if (costItemRepo.count() < 50) seedCostItems();
        if (allocationRepo.count() < 20) seedAllocations();
        if (auditLogRepo.count() < 50) seedAuditLogs();
    }

    // ───────────────────── Departments (8) ─────────────────────
    private static final String[][] DEPT_DATA = {
        {"HQ1", "경영기획본부"},
        {"HQ2", "자산운용본부"},
        {"HQ3", "IT본부"},
        {"HQ4", "리스크관리본부"},
        {"HQ5", "컴플라이언스본부"},
        {"HQ6", "재무회계본부"},
        {"HQ7", "인사총무본부"},
        {"HQ8", "디지털혁신본부"},
    };

    private void seedDepartments() {
        for (String[] d : DEPT_DATA) {
            deptRepo.save(Department.builder().code(d[0]).name(d[1]).build());
        }
    }

    // ───────────────────── Users ─────────────────────
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

    // ───────────────────── Employees (~150) ─────────────────────

    private static final String[] LAST_NAMES = {
        "김","이","박","최","정","강","조","윤","임","한",
        "오","신","홍","송","문","노","구","유","곽","백",
        "권","전","류","서","남","채","지","우","안","배",
        "심","석","민","도","함","진","피","감","반","금",
        "단","갈","맹","설","방","탁","모","여","하","변",
    };
    private static final String[] FIRST_NAMES = {
        "민수","영희","지훈","서연","우진","하늘","민재","서아","도현","지민",
        "은서","유진","기범","채원","수아","태현","민호","나래","재훈","승호",
        "다은","소희","준영","가영","현우","윤석","은별","상현","세빈","준혁",
        "예린","미라","호준","가현","재민","은수","세호","리아","지효","영준",
        "수현","혁진","지현","정훈","선아","하은","우빈","예진","승현","경한",
        "시우","유나","준서","하린","도윤","서율","주원","수빈","연우","하율",
    };

    private static final String[] GRADES = {"사원","대리","과장","차장","부장"};
    private static final int[] GRADE_RATES = {25000, 35000, 50000, 65000, 85000};
    // Distribution per dept: ~40% 사원, ~25% 대리, ~18% 과장, ~10% 차장, ~7% 부장
    private static final double[] GRADE_DIST = {0.40, 0.25, 0.18, 0.10, 0.07};

    private void seedEmployees() {
        List<Department> depts = deptRepo.findAll();
        // Target: 16~22 per dept ≈ 150 total
        int[] perDept = {20, 22, 24, 18, 16, 18, 16, 16};
        int seq = 1;
        int nameIdx = 0;
        for (int di = 0; di < depts.size() && di < perDept.length; di++) {
            Department dept = depts.get(di);
            int count = perDept[di];
            for (int i = 0; i < count; i++) {
                String empNo = String.format("E%04d", seq);
                // check dup
                final String checkNo = empNo;
                if (employeeRepo.findAll().stream().anyMatch(e -> e.getEmpNo().equals(checkNo))) {
                    seq++; nameIdx++; continue;
                }
                int gradeIdx = pickGrade();
                String name = LAST_NAMES[nameIdx % LAST_NAMES.length]
                    + FIRST_NAMES[nameIdx % FIRST_NAMES.length];
                employeeRepo.save(Employee.builder()
                    .empNo(empNo)
                    .name(name)
                    .grade(GRADES[gradeIdx])
                    .department(dept)
                    .hourlyRate(BigDecimal.valueOf(GRADE_RATES[gradeIdx] + rng.nextInt(5) * 1000))
                    .build());
                seq++;
                nameIdx++;
            }
        }
    }

    private int pickGrade() {
        double r = rng.nextDouble();
        double cum = 0;
        for (int i = 0; i < GRADE_DIST.length; i++) {
            cum += GRADE_DIST[i];
            if (r < cum) return i;
        }
        return 0;
    }

    // ───────────────────── Projects (35) ─────────────────────

    private static final String[][] PROJECT_DATA = {
        // code, name, deptCode, budgetHours, budgetCost(만)
        {"PRJ-001","경영전략 수립","HQ1","1600","10000"},
        {"PRJ-002","중장기 사업계획","HQ1","1400","9000"},
        {"PRJ-003","예산편성 시스템","HQ1","1200","8000"},
        {"PRJ-004","조직혁신 프로젝트","HQ1","1000","7000"},
        {"PRJ-005","자산배분 모델링","HQ2","1800","12000"},
        {"PRJ-006","대체투자 운용","HQ2","1600","11000"},
        {"PRJ-007","글로벌 펀드 운용","HQ2","1500","10500"},
        {"PRJ-008","채권 포트폴리오 최적화","HQ2","1400","9500"},
        {"PRJ-009","코어뱅킹 차세대","HQ3","2400","18000"},
        {"PRJ-010","클라우드 마이그레이션","HQ3","2000","15000"},
        {"PRJ-011","데이터 플랫폼 구축","HQ3","1800","14000"},
        {"PRJ-012","보안관제 고도화","HQ3","1500","11000"},
        {"PRJ-013","VaR 모델 개선","HQ4","1400","9500"},
        {"PRJ-014","신용리스크 관리","HQ4","1300","9000"},
        {"PRJ-015","시장리스크 한도관리","HQ4","1200","8500"},
        {"PRJ-016","스트레스 테스트","HQ4","1100","8000"},
        {"PRJ-017","내부통제 점검","HQ5","1200","8500"},
        {"PRJ-018","자금세탁방지 (AML)","HQ5","1400","10000"},
        {"PRJ-019","법규 변경 대응","HQ5","1100","8000"},
        {"PRJ-020","컴플라이언스 교육","HQ5","900","6000"},
        {"PRJ-021","연결재무제표 자동화","HQ6","1300","9200"},
        {"PRJ-022","세무 리스크 분석","HQ6","1100","7800"},
        {"PRJ-023","내부회계관리제도 고도화","HQ6","1500","10500"},
        {"PRJ-024","원가관리 시스템 구축","HQ6","1600","11500"},
        {"PRJ-025","인사평가 시스템 개편","HQ7","1000","7000"},
        {"PRJ-026","복리후생 플랫폼","HQ7","800","5500"},
        {"PRJ-027","사옥 리모델링","HQ7","600","4500"},
        {"PRJ-028","채용 프로세스 혁신","HQ7","700","5000"},
        {"PRJ-029","AI 챗봇 도입","HQ8","1800","13000"},
        {"PRJ-030","RPA 업무 자동화","HQ8","1500","11000"},
        {"PRJ-031","디지털 고객경험 개선","HQ8","1400","10000"},
        {"PRJ-032","블록체인 결제 PoC","HQ8","1000","8000"},
        {"PRJ-033","ESG 경영 대응","HQ1","900","6500"},
        {"PRJ-034","해외법인 관리 체계","HQ2","1100","8000"},
        {"PRJ-035","차세대 데이터센터","HQ3","2200","16000"},
    };

    private void seedProjects() {
        Map<String, Department> deptMap = deptRepo.findAll().stream()
            .collect(Collectors.toMap(Department::getCode, d -> d));
        Set<String> existing = projectRepo.findAll().stream()
            .map(Project::getCode).collect(Collectors.toSet());

        Random pr = new Random(77);
        for (String[] p : PROJECT_DATA) {
            if (existing.contains(p[0])) continue;
            Department dept = deptMap.get(p[2]);
            if (dept == null) continue;

            int startYear = 2024 + pr.nextInt(2);
            int startMonth = 1 + pr.nextInt(12);
            int duration = 6 + pr.nextInt(19);
            LocalDate start = LocalDate.of(startYear, startMonth, 1);
            LocalDate end = start.plusMonths(duration).minusDays(1);

            projectRepo.save(Project.builder()
                .code(p[0])
                .name(p[1])
                .ownerDepartment(dept)
                .budgetHours(new BigDecimal(p[3]))
                .budgetCost(new BigDecimal(p[4]).multiply(BigDecimal.valueOf(10000)))
                .startDate(start)
                .endDate(end)
                .build());
        }
    }

    // ───────────────────── Standard Rates (12 months × 5 grades) ─────────────────────
    private void seedStandardRates() {
        // Gradual rate increases over time
        int[][] ratesByMonth = {
            // 사원, 대리, 과장, 차장, 부장
            {24000, 33000, 48000, 63000, 82000}, // 2025-07
            {24000, 33000, 48000, 63000, 82000}, // 2025-08
            {24000, 33000, 48000, 63000, 82000}, // 2025-09
            {24000, 33000, 48000, 63000, 82000}, // 2025-10
            {24000, 33000, 48000, 63000, 82000}, // 2025-11
            {24000, 33000, 48000, 63000, 82000}, // 2025-12
            {24500, 34000, 49000, 64000, 83000}, // 2026-01
            {24500, 34000, 49000, 64000, 83000}, // 2026-02
            {25000, 35000, 50000, 65000, 85000}, // 2026-03
            {25000, 35000, 50000, 65000, 85000}, // 2026-04
            {25000, 35000, 50000, 65000, 85000}, // 2026-05
            {25000, 35000, 50000, 65000, 85000}, // 2026-06
        };
        String[] months = {
            "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
            "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
        };
        for (int mi = 0; mi < months.length; mi++) {
            for (int gi = 0; gi < GRADES.length; gi++) {
                rateRepo.save(StandardRate.builder()
                    .yearMonth(months[mi])
                    .grade(GRADES[gi])
                    .hourlyRate(BigDecimal.valueOf(ratesByMonth[mi][gi]))
                    .build());
            }
        }
    }

    // ───────────────────── Timesheets (12 months, ~3000–5000 entries) ─────────────────────
    private void seedTimesheets() {
        List<Employee> allEmps = employeeRepo.findAll();
        List<Project> allProjs = projectRepo.findAll();
        if (allEmps.isEmpty() || allProjs.isEmpty()) return;

        // Group employees by department
        Map<Long, List<Employee>> empsByDept = allEmps.stream()
            .collect(Collectors.groupingBy(e -> e.getDepartment().getId()));

        // Group projects by owner department
        Map<Long, List<Project>> projsByDept = allProjs.stream()
            .collect(Collectors.groupingBy(p -> p.getOwnerDepartment().getId()));

        String[] months = {
            "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
            "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
        };

        Random tsRng = new Random(123);

        for (String ym : months) {
            int year = Integer.parseInt(ym.substring(0, 4));
            int month = Integer.parseInt(ym.substring(5, 7));

            // Participation rate increases over time (company growing)
            double participation = 0.55 + (month <= 6 && year == 2026 ? 0.25 : 0.10)
                + tsRng.nextDouble() * 0.10;

            for (Map.Entry<Long, List<Employee>> entry : empsByDept.entrySet()) {
                Long deptId = entry.getKey();
                List<Employee> deptEmps = entry.getValue();
                List<Project> deptProjs = projsByDept.getOrDefault(deptId, Collections.emptyList());

                // Some employees also work on cross-dept projects
                List<Project> crossProjs = allProjs.stream()
                    .filter(p -> !p.getOwnerDepartment().getId().equals(deptId))
                    .collect(Collectors.toList());

                for (Employee emp : deptEmps) {
                    if (tsRng.nextDouble() > participation) continue;

                    // Pick 1-2 projects
                    Project mainProj = deptProjs.isEmpty()
                        ? allProjs.get(tsRng.nextInt(allProjs.size()))
                        : deptProjs.get(tsRng.nextInt(deptProjs.size()));

                    // Check project is active in this month
                    LocalDate monthStart = LocalDate.of(year, month, 1);
                    if (mainProj.getEndDate() != null && mainProj.getEndDate().isBefore(monthStart)) {
                        mainProj = allProjs.get(tsRng.nextInt(allProjs.size()));
                    }

                    // Monthly hours: 사원 140-170h, 대리 150-175h, 과장 130-160h, 차장 120-150h, 부장 80-120h
                    int baseHours = switch (emp.getGrade()) {
                        case "사원" -> 140 + tsRng.nextInt(31);
                        case "대리" -> 150 + tsRng.nextInt(26);
                        case "과장" -> 130 + tsRng.nextInt(31);
                        case "차장" -> 120 + tsRng.nextInt(31);
                        case "부장" -> 80 + tsRng.nextInt(41);
                        default -> 120 + tsRng.nextInt(41);
                    };

                    // Most recent month: some SUBMITTED and DRAFT
                    Timesheet.Status status;
                    if ("2026-06".equals(ym)) {
                        double sr = tsRng.nextDouble();
                        status = sr < 0.3 ? Timesheet.Status.DRAFT
                               : sr < 0.6 ? Timesheet.Status.SUBMITTED
                               : Timesheet.Status.APPROVED;
                    } else if ("2026-05".equals(ym)) {
                        status = tsRng.nextDouble() < 0.15
                            ? Timesheet.Status.SUBMITTED
                            : Timesheet.Status.APPROVED;
                    } else {
                        status = Timesheet.Status.APPROVED;
                    }

                    // 20% chance of working on a second (cross-dept) project
                    boolean hasCross = tsRng.nextDouble() < 0.20 && !crossProjs.isEmpty();
                    int mainHours = hasCross ? (int)(baseHours * 0.7) : baseHours;
                    int crossHours = hasCross ? baseHours - mainHours : 0;

                    spreadTimesheet(emp, mainProj, year, month, mainHours, status, tsRng);
                    if (hasCross) {
                        Project cp = crossProjs.get(tsRng.nextInt(crossProjs.size()));
                        spreadTimesheet(emp, cp, year, month, crossHours, status, tsRng);
                    }
                }
            }
        }
    }

    private void spreadTimesheet(Employee emp, Project proj, int year, int month,
                                  int totalHours, Timesheet.Status status, Random r) {
        int remaining = totalHours;
        // Collect work days in the month
        List<LocalDate> workDays = new ArrayList<>();
        LocalDate d = LocalDate.of(year, month, 1);
        while (d.getMonthValue() == month) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workDays.add(d);
            }
            d = d.plusDays(1);
        }
        if (workDays.isEmpty()) return;

        int dayIdx = 0;
        while (remaining > 0 && dayIdx < workDays.size()) {
            int h = Math.min(remaining, 6 + r.nextInt(3)); // 6-8h per day
            LocalDate date = workDays.get(dayIdx);

            String memo = pickMemo(r);

            Timesheet.TimesheetBuilder tb = Timesheet.builder()
                .employee(emp).project(proj).workDate(date)
                .hours(BigDecimal.valueOf(h)).memo(memo)
                .status(status);

            if (status == Timesheet.Status.SUBMITTED || status == Timesheet.Status.APPROVED) {
                tb.submittedAt(date.atTime(17 + r.nextInt(2), r.nextInt(60)));
            }
            if (status == Timesheet.Status.APPROVED) {
                tb.approvedAt(date.plusDays(1 + r.nextInt(3)).atTime(9 + r.nextInt(3), r.nextInt(60)))
                  .approvedByEmail("manager@noaats.com");
            }

            timesheetRepo.save(tb.build());
            remaining -= h;
            dayIdx++;
        }
    }

    private static final String[] MEMOS = {
        "설계 검토","코드 리뷰","회의 참석","문서 작성","테스트 수행",
        "요구사항 분석","데이터 분석","모델 개발","시스템 점검","고객 미팅",
        "기술 검토","아키텍처 설계","프로토타입 개발","성능 최적화","보안 점검",
        "배포 작업","운영 지원","교육 진행","보고서 작성","기획서 작성",
        "리서치","PoC 개발","마이그레이션 작업","모니터링","장애 대응",
        "인터페이스 개발","API 연동","DB 설계","UI/UX 개선","법규 검토",
    };

    private String pickMemo(Random r) {
        return MEMOS[r.nextInt(MEMOS.length)];
    }

    // ───────────────────── Cost Items (indirect costs per month) ─────────────────────
    private void seedCostItems() {
        List<Department> depts = deptRepo.findAll();
        String[] months = {
            "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
            "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
        };
        String[][] categories = {
            {"사무실 임대료","INDIRECT"},
            {"공과금","INDIRECT"},
            {"통신비","INDIRECT"},
            {"소모품비","INDIRECT"},
            {"교육훈련비","INDIRECT"},
            {"복리후생비","INDIRECT"},
            {"감가상각비","INDIRECT"},
            {"보험료","INDIRECT"},
        };
        long[][] amounts = {
            // per dept, per category (in 만 won). Vary by dept size
            {1200, 180, 90, 60, 120, 200, 300, 150},  // HQ1
            {1400, 200, 100, 70, 140, 240, 350, 170},  // HQ2
            {1600, 250, 150, 100, 180, 280, 400, 200},  // HQ3 (IT - larger)
            {1000, 150, 80, 50, 100, 180, 250, 130},   // HQ4
            {900, 130, 70, 45, 90, 160, 220, 110},     // HQ5
            {1100, 170, 85, 55, 110, 190, 280, 140},   // HQ6
            {800, 120, 65, 40, 80, 150, 200, 100},     // HQ7
            {1300, 200, 120, 80, 150, 220, 320, 160},  // HQ8
        };
        Random ciRng = new Random(55);
        for (String ym : months) {
            for (int di = 0; di < depts.size() && di < amounts.length; di++) {
                Department dept = depts.get(di);
                for (int ci = 0; ci < categories.length; ci++) {
                    // Add ±10% random variation per month
                    double variation = 0.90 + ciRng.nextDouble() * 0.20;
                    long amt = (long)(amounts[di][ci] * 10000 * variation);
                    costItemRepo.save(CostItem.builder()
                        .yearMonth(ym)
                        .type(CostItem.CostType.INDIRECT)
                        .department(dept)
                        .category(categories[ci][0])
                        .amount(BigDecimal.valueOf(amt))
                        .build());
                }
            }
        }
    }

    // ───────────────────── Allocations ─────────────────────
    private void seedAllocations() {
        List<Department> depts = deptRepo.findAll();
        List<Project> projs = projectRepo.findAll();
        if (depts.isEmpty() || projs.isEmpty()) return;

        String[] months = {
            "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
            "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
        };
        Random alRng = new Random(88);
        CostAllocation.AllocationBasis[] bases = CostAllocation.AllocationBasis.values();

        for (String ym : months) {
            // Each dept allocates to 2-4 projects
            for (Department dept : depts) {
                List<Project> deptProjs = projs.stream()
                    .filter(p -> p.getOwnerDepartment().getId().equals(dept.getId()))
                    .collect(Collectors.toList());
                if (deptProjs.isEmpty()) continue;

                int allocCount = 2 + alRng.nextInt(3);
                for (int i = 0; i < allocCount && i < deptProjs.size(); i++) {
                    long amount = (5_000_000L + alRng.nextInt(15_000_000));
                    allocationRepo.save(CostAllocation.builder()
                        .yearMonth(ym)
                        .sourceDepartment(dept)
                        .targetProject(deptProjs.get(i))
                        .basis(bases[alRng.nextInt(bases.length)])
                        .amount(BigDecimal.valueOf(amount))
                        .kind(CostAllocation.AllocationKind.STANDARD_ALLOC)
                        .createdAt(LocalDate.parse(ym + "-15").atTime(10, 0))
                        .build());
                }
            }

            // 2-3 inter-dept transfers per month
            int transferCount = 2 + alRng.nextInt(2);
            for (int t = 0; t < transferCount; t++) {
                Department src = depts.get(alRng.nextInt(depts.size()));
                Department tgt;
                do { tgt = depts.get(alRng.nextInt(depts.size())); } while (tgt.getId().equals(src.getId()));

                int hours = 20 + alRng.nextInt(60);
                int rate = 50000 + alRng.nextInt(30000);
                long amount = (long) hours * rate;

                String[] transferMemos = {
                    "IT 개발지원","경영기획 자문","리스크 분석 지원","컴플라이언스 검토",
                    "데이터 분석 지원","시스템 운영 지원","보안 점검 지원","교육 지원",
                };

                allocationRepo.save(CostAllocation.builder()
                    .yearMonth(ym)
                    .sourceDepartment(src)
                    .targetDepartment(tgt)
                    .basis(CostAllocation.AllocationBasis.HOURS)
                    .amount(BigDecimal.valueOf(amount))
                    .kind(CostAllocation.AllocationKind.TRANSFER)
                    .memo(transferMemos[alRng.nextInt(transferMemos.length)]
                        + " (" + hours + "h × " + String.format("%,d", rate) + "원)")
                    .createdAt(LocalDate.parse(ym + "-20").atTime(14, 0))
                    .build());
            }
        }
    }

    // ───────────────────── Audit Logs ─────────────────────
    private void seedAuditLogs() {
        String[] actors = {"admin@noaats.com", "manager@noaats.com", "user@noaats.com"};
        String[] actions = {
            "CREATE_TIMESHEET","SUBMIT_TIMESHEET","APPROVE_TIMESHEET","REJECT_TIMESHEET",
            "DELETE_TIMESHEET","ALLOCATE","TRANSFER",
        };
        String[] entities = {"TIMESHEET","TIMESHEET","TIMESHEET","TIMESHEET","TIMESHEET","ALLOCATION","ALLOCATION"};

        Random auRng = new Random(99);
        // Generate ~200 audit entries over 12 months
        for (int i = 0; i < 200; i++) {
            int monthOffset = auRng.nextInt(12);
            LocalDate base = LocalDate.of(2025, 7, 1).plusMonths(monthOffset);
            int day = 1 + auRng.nextInt(28);
            LocalDateTime ts = base.withDayOfMonth(Math.min(day, base.lengthOfMonth()))
                .atTime(8 + auRng.nextInt(10), auRng.nextInt(60));

            int actionIdx = auRng.nextInt(actions.length);
            String detail = switch (actions[actionIdx]) {
                case "CREATE_TIMESHEET" -> "공수 등록 " + (4 + auRng.nextInt(5)) + "h";
                case "SUBMIT_TIMESHEET" -> "공수 제출";
                case "APPROVE_TIMESHEET" -> "공수 승인 완료";
                case "REJECT_TIMESHEET" -> "반려 사유: 프로젝트 코드 확인 필요";
                case "DELETE_TIMESHEET" -> "DRAFT 공수 삭제";
                case "ALLOCATE" -> "간접비 배분 실행 (" + base.getYear() + "-"
                    + String.format("%02d", base.getMonthValue()) + ")";
                case "TRANSFER" -> "내부대체 기록";
                default -> "";
            };

            auditLogRepo.save(AuditLog.builder()
                .actor(actors[auRng.nextInt(actors.length)])
                .action(actions[actionIdx])
                .entity(entities[actionIdx])
                .entityId((long)(1 + auRng.nextInt(500)))
                .detail(detail)
                .createdAt(ts)
                .build());
        }
    }

    // ───────────────────── Migration helper ─────────────────────
    private void migrateAllocationKind() {
        allocationRepo.findAll().stream()
            .filter(a -> a.getKind() == null)
            .forEach(a -> {
                a.setKind(CostAllocation.AllocationKind.STANDARD_ALLOC);
                allocationRepo.save(a);
            });
    }
}
