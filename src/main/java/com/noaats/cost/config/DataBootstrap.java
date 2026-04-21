package com.noaats.cost.config;

import com.noaats.cost.domain.*;
import com.noaats.cost.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Realistic demo data for a large financial company (~2000 employees).
 * Uses JdbcTemplate batch inserts for fast seeding on remote DB.
 */
@Slf4j
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
    private final JdbcTemplate jdbc;

    private final Random rng = new Random(42);

    @Override
    public void run(String... args) {
        migrateAllocationKind();
        if (deptRepo.count() < 5) {
            // On H2 (local), data-h2.sql may have seeded conflicting data — wipe it
            try {
                jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
                jdbc.execute("DELETE FROM audit_log");
                jdbc.execute("DELETE FROM cost_allocation");
                jdbc.execute("DELETE FROM cost_item");
                jdbc.execute("DELETE FROM timesheet");
                jdbc.execute("DELETE FROM standard_rate");
                jdbc.execute("DELETE FROM employee");
                jdbc.execute("DELETE FROM project");
                jdbc.execute("DELETE FROM department");
                jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
            } catch (Exception e) {
                log.debug("H2 cleanup skipped (not H2): {}", e.getMessage());
            }
            seedDepartments();
        }
        if (userRepo.count() == 0) seedUsers();
        if (employeeRepo.count() < 40) { log.info("Seeding 50 employees..."); seedEmployeesFast(); }
        if (projectRepo.count() < 15) { log.info("Seeding 20 projects..."); seedProjectsFast(); }
        if (rateRepo.count() < 70) seedStandardRatesFast();
        if (timesheetRepo.count() < 500) { log.info("Seeding timesheets (JDBC batch)..."); seedTimesheetsFast(); }
        if (costItemRepo.count() < 30) { log.info("Seeding cost items..."); seedCostItemsFast(); }
        if (allocationRepo.count() < 20) { log.info("Seeding allocations..."); seedAllocationsFast(); }
        if (auditLogRepo.count() < 100) seedAuditLogsFast();
        log.info("DataBootstrap complete.");
    }

    // ───────────────────── Departments (20) ─────────────────────
    private static final String[][] DEPT_DATA = {
        {"HQ01","경영기획본부"},{"HQ02","IT본부"},{"HQ03","재무회계본부"},
        {"HQ04","영업본부"},{"HQ05","리스크관리본부"},
    };

    private void seedDepartments() {
        List<Department> list = new ArrayList<>();
        for (String[] d : DEPT_DATA) list.add(Department.builder().code(d[0]).name(d[1]).build());
        deptRepo.saveAll(list);
    }

    private void seedUsers() {
        String hash = encoder.encode("password123");
        List<Department> depts = deptRepo.findAll();
        userRepo.saveAll(List.of(
            User.builder().email("admin@noaats.com").password(hash).name("관리자")
                .role(Role.ADMIN).department(depts.get(0)).build(),
            User.builder().email("manager@noaats.com").password(hash).name("본부장")
                .role(Role.MANAGER).department(depts.get(1)).build(),
            User.builder().email("user@noaats.com").password(hash).name("일반사용자")
                .role(Role.USER).department(depts.get(2)).build()
        ));
    }

    // ───────────────────── Name/Grade data ─────────────────────
    private static final String[] LAST_NAMES = {
        "김","이","박","최","정","강","조","윤","임","한","오","신","홍","송","문",
        "노","구","유","곽","백","권","전","류","서","남","채","지","우","안","배",
        "심","석","민","도","함","진","피","감","반","금","단","갈","맹","설","방",
        "탁","모","여","하","변",
    };
    private static final String[] FIRST_NAMES = {
        "민수","영희","지훈","서연","우진","하늘","민재","서아","도현","지민",
        "은서","유진","기범","채원","수아","태현","민호","나래","재훈","승호",
        "다은","소희","준영","가영","현우","윤석","은별","상현","세빈","준혁",
        "예린","미라","호준","가현","재민","은수","세호","리아","지효","영준",
        "수현","혁진","지현","정훈","선아","하은","우빈","예진","승현","경한",
        "시우","유나","준서","하린","도윤","서율","주원","수빈","연우","하율",
        "태민","소연","성민","지우","건우","서현","찬영","하영","동현","보람",
        "유정","정민","현서","시현","은호","선우","규민","다현","세진","원준",
        "지영","윤호","태영","수진","승민","채린","하준","가은","민영","현진",
        "서준","예나","유빈","태윤","지수","은채","준하","소율","현성","동윤",
    };
    private static final String[] GRADES = {"사원","대리","과장","차장","부장"};
    private static final int[] GRADE_RATES = {25000, 35000, 50000, 65000, 85000};
    private static final double[] GRADE_DIST = {0.40, 0.25, 0.18, 0.10, 0.07};

    private int pickGrade() {
        double r = rng.nextDouble(); double cum = 0;
        for (int i = 0; i < GRADE_DIST.length; i++) { cum += GRADE_DIST[i]; if (r < cum) return i; }
        return 0;
    }

    // ───────────────────── Employees JDBC batch ─────────────────────
    private void seedEmployeesFast() {
        List<Department> depts = deptRepo.findAll();
        Map<String, Long> deptIdByCode = depts.stream().collect(Collectors.toMap(Department::getCode, Department::getId));
        int[] perDept = {12,10,10,10,8};

        List<Object[]> rows = new ArrayList<>(2100);
        int seq = 1, nameIdx = 0;
        for (int di = 0; di < DEPT_DATA.length && di < perDept.length; di++) {
            Long deptId = deptIdByCode.get(DEPT_DATA[di][0]);
            for (int i = 0; i < perDept[di]; i++) {
                int gi = pickGrade();
                rows.add(new Object[]{
                    String.format("E%04d", seq),
                    LAST_NAMES[nameIdx % LAST_NAMES.length] + FIRST_NAMES[nameIdx % FIRST_NAMES.length],
                    GRADES[gi], deptId, BigDecimal.valueOf(GRADE_RATES[gi] + rng.nextInt(5) * 1000)
                });
                seq++; nameIdx++;
            }
        }
        jdbc.batchUpdate("INSERT INTO employee (emp_no, name, grade, department_id, hourly_rate) VALUES (?,?,?,?,?)",
            rows, 500, (ps, row) -> {
                ps.setString(1, (String)row[0]); ps.setString(2, (String)row[1]);
                ps.setString(3, (String)row[2]); ps.setLong(4, (Long)row[3]);
                ps.setBigDecimal(5, (BigDecimal)row[4]);
            });
        log.info("Seeded {} employees", rows.size());
    }

    // ───────────────────── Projects JDBC batch ─────────────────────
    private static final String[][] PROJECT_DATA = {
        // HQ01 경영기획본부 (4 projects)
        {"PRJ-001","경영전략 수립","HQ01","2400","16000"},
        {"PRJ-002","중장기 사업계획","HQ01","2000","14000"},
        {"PRJ-003","예산편성 시스템","HQ01","1800","12000"},
        {"PRJ-004","조직혁신 프로젝트","HQ01","1600","11000"},
        // HQ02 IT본부 (4 projects)
        {"PRJ-005","코어뱅킹 차세대","HQ02","3200","24000"},
        {"PRJ-006","클라우드 마이그레이션","HQ02","2800","20000"},
        {"PRJ-007","데이터 플랫폼 구축","HQ02","2400","18000"},
        {"PRJ-008","보안관제 고도화","HQ02","2000","15000"},
        // HQ03 재무회계본부 (4 projects)
        {"PRJ-009","연결재무제표 자동화","HQ03","1600","11500"},
        {"PRJ-010","내부회계관리제도 고도화","HQ03","1800","13000"},
        {"PRJ-011","원가관리 시스템 구축","HQ03","2000","14500"},
        {"PRJ-012","세무 리스크 분석","HQ03","1400","10000"},
        // HQ04 영업본부 (4 projects)
        {"PRJ-013","CRM 고도화","HQ04","2000","14500"},
        {"PRJ-014","영업채널 다각화","HQ04","1600","11500"},
        {"PRJ-015","고객 세그먼트 분석","HQ04","1400","10000"},
        {"PRJ-016","영업실적 대시보드","HQ04","1200","8500"},
        // HQ05 리스크관리본부 (4 projects)
        {"PRJ-017","VaR 모델 개선","HQ05","1800","12500"},
        {"PRJ-018","신용리스크 관리","HQ05","1600","11500"},
        {"PRJ-019","시장리스크 한도관리","HQ05","1400","10000"},
        {"PRJ-020","스트레스 테스트","HQ05","1300","9000"},
    };

    private void seedProjectsFast() {
        Map<String, Long> deptIdByCode = deptRepo.findAll().stream()
            .collect(Collectors.toMap(Department::getCode, Department::getId));
        Random pr = new Random(77);
        List<Object[]> rows = new ArrayList<>();
        for (String[] p : PROJECT_DATA) {
            Long deptId = deptIdByCode.get(p[2]);
            if (deptId == null) continue;
            int startMonth = 6 + pr.nextInt(13);
            LocalDate start = LocalDate.of(2024, 1, 1).plusMonths(startMonth - 1).withDayOfMonth(1);
            LocalDate end = start.plusMonths(8 + pr.nextInt(16)).minusDays(1);
            if (end.isBefore(LocalDate.of(2026, 4, 30))) end = LocalDate.of(2026, 4, 30);
            rows.add(new Object[]{p[0], p[1], deptId, new BigDecimal(p[3]),
                new BigDecimal(p[4]).multiply(BigDecimal.valueOf(10000)),
                java.sql.Date.valueOf(start), java.sql.Date.valueOf(end)});
        }
        jdbc.batchUpdate("INSERT INTO project (code, name, owner_department_id, budget_hours, budget_cost, start_date, end_date) VALUES (?,?,?,?,?,?,?)",
            rows, 100, (ps, row) -> {
                ps.setString(1,(String)row[0]); ps.setString(2,(String)row[1]); ps.setLong(3,(Long)row[2]);
                ps.setBigDecimal(4,(BigDecimal)row[3]); ps.setBigDecimal(5,(BigDecimal)row[4]);
                ps.setDate(6,(java.sql.Date)row[5]); ps.setDate(7,(java.sql.Date)row[6]);
            });
        log.info("Seeded {} projects", rows.size());
    }

    // ───────────────────── Standard Rates ─────────────────────
    private static final String[] MONTHS = {
        "2025-01","2025-02","2025-03","2025-04","2025-05","2025-06",
        "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
        "2026-01","2026-02","2026-03","2026-04",
    };

    private void seedStandardRatesFast() {
        int[][] rt = {
            {23000,32000,47000,62000,80000},{23000,32000,47000,62000,80000},{23000,32000,47000,62000,80000},
            {23500,32500,47500,62500,81000},{23500,32500,47500,62500,81000},{23500,32500,47500,62500,81000},
            {24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},
            {24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},
            {25000,35000,50000,65000,85000},{25000,35000,50000,65000,85000},{25000,35000,50000,65000,85000},{25000,35000,50000,65000,85000},
        };
        List<Object[]> rows = new ArrayList<>();
        for (int mi = 0; mi < MONTHS.length; mi++)
            for (int gi = 0; gi < GRADES.length; gi++)
                rows.add(new Object[]{MONTHS[mi], GRADES[gi], BigDecimal.valueOf(rt[mi][gi])});
        jdbc.batchUpdate("INSERT INTO standard_rate (year_month_val, grade, hourly_rate) VALUES (?,?,?)",
            rows, 100, (ps, row) -> {
                ps.setString(1,(String)row[0]); ps.setString(2,(String)row[1]); ps.setBigDecimal(3,(BigDecimal)row[2]);
            });
    }

    // ───────────────────── Timesheets JDBC batch ─────────────────────
    private void seedTimesheetsFast() {
        List<Map<String,Object>> emps = jdbc.queryForList("SELECT id, department_id, grade FROM employee");
        List<Map<String,Object>> projs = jdbc.queryForList("SELECT id, owner_department_id FROM project");

        Map<Long,List<Map<String,Object>>> empsByDept = emps.stream()
            .collect(Collectors.groupingBy(e -> ((Number)e.get("department_id")).longValue()));
        Map<Long,List<Long>> projIdsByDept = projs.stream()
            .collect(Collectors.groupingBy(p -> ((Number)p.get("owner_department_id")).longValue(),
                Collectors.mapping(p -> ((Number)p.get("id")).longValue(), Collectors.toList())));
        List<Long> allProjIds = projs.stream().map(p -> ((Number)p.get("id")).longValue()).collect(Collectors.toList());

        // Company-wide flagship projects: PRJ-005 코어뱅킹 차세대, PRJ-006 클라우드 마이그레이션, PRJ-011 원가관리 시스템
        List<Long> flagshipIds = new ArrayList<>();
        for (Map<String,Object> p : projs) {
            String code = jdbc.queryForObject("SELECT code FROM project WHERE id=?", String.class, ((Number)p.get("id")).longValue());
            if (code != null && (code.equals("PRJ-005") || code.equals("PRJ-006") || code.equals("PRJ-011")))
                flagshipIds.add(((Number)p.get("id")).longValue());
        }

        Random tsRng = new Random(123);
        List<Object[]> batch = new ArrayList<>(2000);
        int total = 0;

        String[] memos = {"설계 검토","코드 리뷰","회의 참석","문서 작성","테스트 수행",
            "요구사항 분석","데이터 분석","모델 개발","시스템 점검","고객 미팅",
            "기술 검토","아키텍처 설계","프로토타입 개발","성능 최적화","보안 점검",
            "배포 작업","운영 지원","교육 진행","보고서 작성","기획서 작성",
            "리서치","PoC 개발","마이그레이션 작업","모니터링","장애 대응"};

        for (String ym : MONTHS) {
            int year = Integer.parseInt(ym.substring(0,4));
            int month = Integer.parseInt(ym.substring(5,7));

            double participation;
            if (year == 2025 && month <= 3) participation = 0.25 + tsRng.nextDouble() * 0.05;
            else if (year == 2025 && month <= 6) participation = 0.30 + tsRng.nextDouble() * 0.05;
            else if (year == 2025) participation = 0.35 + tsRng.nextDouble() * 0.05;
            else participation = 0.40 + tsRng.nextDouble() * 0.05;
            if ("2026-04".equals(ym)) participation *= 0.6;

            // Collect working days (Mon-Fri) for this month
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            if ("2026-04".equals(ym)) lastDay = LocalDate.of(2026, 4, 21);
            List<LocalDate> workDays = new ArrayList<>();
            for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
                if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                    workDays.add(d);
            }

            for (Map.Entry<Long,List<Map<String,Object>>> entry : empsByDept.entrySet()) {
                Long deptId = entry.getKey();
                List<Long> deptProjIds = projIdsByDept.getOrDefault(deptId, allProjIds);

                for (Map<String,Object> emp : entry.getValue()) {
                    if (tsRng.nextDouble() > participation) continue;
                    Long empId = ((Number)emp.get("id")).longValue();

                    // 40% → flagship company-wide project (creates dominant slices in pie chart)
                    // 40% → own department's first project (department flagship)
                    // 20% → random project from own department
                    Long projId;
                    double roll = tsRng.nextDouble();
                    if (roll < 0.40 && !flagshipIds.isEmpty()) {
                        projId = flagshipIds.get(tsRng.nextInt(flagshipIds.size()));
                    } else if (roll < 0.80) {
                        projId = deptProjIds.get(0); // department's main project
                    } else {
                        projId = deptProjIds.get(tsRng.nextInt(deptProjIds.size()));
                    }

                    // One random working day per month, 4-8 hours (realistic daily entry)
                    LocalDate day = workDays.get(tsRng.nextInt(workDays.size()));
                    int hours = 4 + tsRng.nextInt(5); // 4~8 hours

                    String status;
                    if ("2026-04".equals(ym)) {
                        double sr = tsRng.nextDouble();
                        status = sr < 0.50 ? "DRAFT" : sr < 0.80 ? "SUBMITTED" : "APPROVED";
                    } else if ("2026-03".equals(ym)) {
                        double sr = tsRng.nextDouble();
                        status = sr < 0.10 ? "DRAFT" : sr < 0.25 ? "SUBMITTED" : "APPROVED";
                    } else { status = "APPROVED"; }

                    java.sql.Timestamp submittedAt = null, approvedAt = null;
                    String approvedBy = null;
                    if (!"DRAFT".equals(status))
                        submittedAt = java.sql.Timestamp.valueOf(day.atTime(17 + tsRng.nextInt(2), tsRng.nextInt(60)));
                    if ("APPROVED".equals(status)) {
                        approvedAt = java.sql.Timestamp.valueOf(day.plusDays(1 + tsRng.nextInt(3)).atTime(9 + tsRng.nextInt(3), tsRng.nextInt(60)));
                        approvedBy = "manager@noaats.com";
                    }

                    batch.add(new Object[]{empId, projId, java.sql.Date.valueOf(day),
                        BigDecimal.valueOf(hours), memos[tsRng.nextInt(memos.length)],
                        status, submittedAt, approvedAt, approvedBy});
                    total++;
                    if (batch.size() >= 1000) { flushTimesheets(batch); batch.clear(); }
                }
            }
            log.info("Timesheets through {}, total: {}", ym, total);
        }
        if (!batch.isEmpty()) flushTimesheets(batch);
        log.info("Seeded {} timesheets total", total);
    }

    private void flushTimesheets(List<Object[]> batch) {
        jdbc.batchUpdate("INSERT INTO timesheet (employee_id, project_id, work_date, hours, memo, status, submitted_at, approved_at, approved_by_email) VALUES (?,?,?,?,?,?,?,?,?)",
            batch, batch.size(), (ps, row) -> {
                ps.setLong(1,(Long)row[0]); ps.setLong(2,(Long)row[1]); ps.setDate(3,(java.sql.Date)row[2]);
                ps.setBigDecimal(4,(BigDecimal)row[3]); ps.setString(5,(String)row[4]); ps.setString(6,(String)row[5]);
                ps.setTimestamp(7,(java.sql.Timestamp)row[6]); ps.setTimestamp(8,(java.sql.Timestamp)row[7]); ps.setString(9,(String)row[8]);
            });
    }

    // ───────────────────── Cost Items JDBC batch ─────────────────────
    private void seedCostItemsFast() {
        List<Department> depts = deptRepo.findAll();
        String[] cats = {"사무실 임대료","공과금","통신비","소모품비","교육훈련비","복리후생비","감가상각비","보험료"};
        long[][] amounts = {
            {1200,180,90,60,120,200,300,150},  // HQ01 경영기획
            {1600,250,150,100,180,280,400,200}, // HQ02 IT
            {1100,170,85,55,110,190,280,140},   // HQ03 재무회계
            {1000,150,80,55,100,180,250,130},   // HQ04 영업
            {900,130,70,45,90,160,220,110},     // HQ05 리스크관리
        };
        Random ciRng = new Random(55);
        List<Object[]> rows = new ArrayList<>();
        for (String ym : MONTHS)
            for (int di = 0; di < depts.size() && di < amounts.length; di++)
                for (int ci = 0; ci < cats.length; ci++) {
                    double v = 0.90 + ciRng.nextDouble() * 0.20;
                    rows.add(new Object[]{ym, "INDIRECT", depts.get(di).getId(), cats[ci],
                        BigDecimal.valueOf((long)(amounts[di][ci] * 10000 * v))});
                }
        jdbc.batchUpdate("INSERT INTO cost_item (year_month_val, type, department_id, category, amount) VALUES (?,?,?,?,?)",
            rows, 500, (ps, row) -> {
                ps.setString(1,(String)row[0]); ps.setString(2,(String)row[1]); ps.setLong(3,(Long)row[2]);
                ps.setString(4,(String)row[3]); ps.setBigDecimal(5,(BigDecimal)row[4]);
            });
        log.info("Seeded {} cost items", rows.size());
    }

    // ───────────────────── Allocations JDBC batch ─────────────────────
    private void seedAllocationsFast() {
        List<Department> depts = deptRepo.findAll();
        Map<Long,List<Long>> projIdsByDept = jdbc.queryForList("SELECT id, owner_department_id FROM project")
            .stream().collect(Collectors.groupingBy(p -> ((Number)p.get("owner_department_id")).longValue(),
                Collectors.mapping(p -> ((Number)p.get("id")).longValue(), Collectors.toList())));

        Random alRng = new Random(88);
        String[] bases = {"HOURS","HEADCOUNT","REVENUE"};
        String[] transferMemos = {"IT 개발지원","경영기획 자문","리스크 분석 지원","컴플라이언스 검토",
            "데이터 분석 지원","시스템 운영 지원","보안 점검 지원","교육 지원",
            "영업 지원","마케팅 자문","법무 검토","감사 지원",
            "글로벌 사업 지원","고객서비스 지원","R&D 협력","ESG 자문"};

        List<Object[]> all = new ArrayList<>();
        for (String ym : MONTHS) {
            java.sql.Timestamp allocTs = java.sql.Timestamp.valueOf(LocalDate.parse(ym+"-15").atTime(10,0));
            java.sql.Timestamp transTs = java.sql.Timestamp.valueOf(LocalDate.parse(ym+"-20").atTime(14,0));

            for (Department dept : depts) {
                List<Long> deptProjIds = projIdsByDept.getOrDefault(dept.getId(), Collections.emptyList());
                if (deptProjIds.isEmpty()) continue;
                int allocCount = 2 + alRng.nextInt(3);
                for (int i = 0; i < allocCount && i < deptProjIds.size(); i++)
                    all.add(new Object[]{ym, dept.getId(), deptProjIds.get(i), null,
                        bases[alRng.nextInt(bases.length)], BigDecimal.valueOf(5_000_000L+alRng.nextInt(20_000_000)),
                        "STANDARD_ALLOC", null, allocTs});
            }
            int tc = 3 + alRng.nextInt(3);
            for (int t = 0; t < tc; t++) {
                Department src = depts.get(alRng.nextInt(depts.size()));
                Department tgt; do { tgt = depts.get(alRng.nextInt(depts.size())); } while (tgt.getId().equals(src.getId()));
                int h = 20+alRng.nextInt(80); int r = 50000+alRng.nextInt(40000);
                all.add(new Object[]{ym, src.getId(), null, tgt.getId(), "HOURS",
                    BigDecimal.valueOf((long)h*r), "TRANSFER",
                    transferMemos[alRng.nextInt(transferMemos.length)]+" ("+h+"h × "+String.format("%,d",r)+"원)", transTs});
            }
        }
        jdbc.batchUpdate("INSERT INTO cost_allocation (year_month_val, source_department_id, target_project_id, target_department_id, basis, amount, kind, memo, created_at) VALUES (?,?,?,?,?,?,?,?,?)",
            all, 500, (ps, row) -> {
                ps.setString(1,(String)row[0]); ps.setLong(2,(Long)row[1]);
                if (row[2]!=null) ps.setLong(3,(Long)row[2]); else ps.setNull(3, java.sql.Types.BIGINT);
                if (row[3]!=null) ps.setLong(4,(Long)row[3]); else ps.setNull(4, java.sql.Types.BIGINT);
                ps.setString(5,(String)row[4]); ps.setBigDecimal(6,(BigDecimal)row[5]);
                ps.setString(7,(String)row[6]); ps.setString(8,(String)row[7]); ps.setTimestamp(9,(java.sql.Timestamp)row[8]);
            });
        log.info("Seeded {} allocations + transfers", all.size());
    }

    // ───────────────────── Audit Logs JDBC batch ─────────────────────
    private void seedAuditLogsFast() {
        String[] actors = {"admin@noaats.com","manager@noaats.com","user@noaats.com"};
        String[] actions = {"CREATE_TIMESHEET","SUBMIT_TIMESHEET","APPROVE_TIMESHEET","REJECT_TIMESHEET","DELETE_TIMESHEET","ALLOCATE","TRANSFER"};
        String[] entities = {"TIMESHEET","TIMESHEET","TIMESHEET","TIMESHEET","TIMESHEET","ALLOCATION","ALLOCATION"};
        Random auRng = new Random(99);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            LocalDate base = LocalDate.of(2025,1,1).plusMonths(auRng.nextInt(16));
            int day = 1+auRng.nextInt(28);
            LocalDateTime ts = base.withDayOfMonth(Math.min(day, base.lengthOfMonth())).atTime(8+auRng.nextInt(10), auRng.nextInt(60));
            int ai = auRng.nextInt(actions.length);
            String detail = switch (actions[ai]) {
                case "CREATE_TIMESHEET" -> "공수 등록 "+(4+auRng.nextInt(5))+"h";
                case "SUBMIT_TIMESHEET" -> "공수 제출"; case "APPROVE_TIMESHEET" -> "공수 승인 완료";
                case "REJECT_TIMESHEET" -> "반려 사유: 프로젝트 코드 확인 필요"; case "DELETE_TIMESHEET" -> "DRAFT 공수 삭제";
                case "ALLOCATE" -> "간접비 배분 실행 ("+base.getYear()+"-"+String.format("%02d",base.getMonthValue())+")";
                case "TRANSFER" -> "내부대체 기록"; default -> "";
            };
            rows.add(new Object[]{actors[auRng.nextInt(actors.length)], actions[ai], entities[ai],
                (long)(1+auRng.nextInt(2000)), detail, java.sql.Timestamp.valueOf(ts)});
        }
        jdbc.batchUpdate("INSERT INTO audit_log (actor, action, entity, entity_id, detail, created_at) VALUES (?,?,?,?,?,?)",
            rows, 500, (ps, row) -> {
                ps.setString(1,(String)row[0]); ps.setString(2,(String)row[1]); ps.setString(3,(String)row[2]);
                ps.setLong(4,(Long)row[3]); ps.setString(5,(String)row[4]); ps.setTimestamp(6,(java.sql.Timestamp)row[5]);
            });
    }

    private void migrateAllocationKind() {
        allocationRepo.findAll().stream().filter(a -> a.getKind() == null).forEach(a -> {
            a.setKind(CostAllocation.AllocationKind.STANDARD_ALLOC); allocationRepo.save(a);
        });
    }
}
