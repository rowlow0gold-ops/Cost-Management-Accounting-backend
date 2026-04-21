package com.noaats.cost.config;

import com.noaats.cost.domain.*;
import com.noaats.cost.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Realistic demo data for a large financial company (~2000 employees).
 * Covers 16 months (2025-01 → 2026-04) with timesheets, cost items,
 * allocations, transfers, and audit logs.
 *
 * Uses saveAll() batch inserts for performance on remote DB (Neon/Render).
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

    private final Random rng = new Random(42);
    private static final int BATCH = 500;

    @Override
    public void run(String... args) {
        migrateAllocationKind();
        if (deptRepo.count() == 0) seedDepartments();
        if (userRepo.count() == 0) seedUsers();
        if (employeeRepo.count() < 1900) { log.info("Seeding employees..."); seedEmployees(); }
        if (projectRepo.count() < 80) { log.info("Seeding projects..."); seedProjects(); }
        if (rateRepo.count() == 0) seedStandardRates();
        if (timesheetRepo.count() < 5000) { log.info("Seeding timesheets..."); seedTimesheets(); }
        if (costItemRepo.count() < 100) { log.info("Seeding cost items..."); seedCostItems(); }
        if (allocationRepo.count() < 50) { log.info("Seeding allocations..."); seedAllocations(); }
        if (auditLogRepo.count() < 100) seedAuditLogs();
        log.info("DataBootstrap complete.");
    }

    // ───────────────────── Departments (20) ─────────────────────
    private static final String[][] DEPT_DATA = {
        {"HQ01", "경영기획본부"},
        {"HQ02", "자산운용본부"},
        {"HQ03", "IT본부"},
        {"HQ04", "리스크관리본부"},
        {"HQ05", "컴플라이언스본부"},
        {"HQ06", "재무회계본부"},
        {"HQ07", "인사총무본부"},
        {"HQ08", "디지털혁신본부"},
        {"HQ09", "영업본부"},
        {"HQ10", "마케팅본부"},
        {"HQ11", "법무본부"},
        {"HQ12", "감사본부"},
        {"HQ13", "글로벌사업본부"},
        {"HQ14", "고객서비스본부"},
        {"HQ15", "연구개발본부"},
        {"HQ16", "데이터분석본부"},
        {"HQ17", "신사업본부"},
        {"HQ18", "자금운용본부"},
        {"HQ19", "부동산본부"},
        {"HQ20", "ESG경영본부"},
    };

    private void seedDepartments() {
        List<Department> list = new ArrayList<>();
        for (String[] d : DEPT_DATA) {
            list.add(Department.builder().code(d[0]).name(d[1]).build());
        }
        deptRepo.saveAll(list);
    }

    // ───────────────────── Users ─────────────────────
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

    // ───────────────────── Employees (2000) ─────────────────────

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
        "태민","소연","성민","지우","건우","서현","찬영","하영","동현","보람",
        "유정","정민","현서","시현","은호","선우","규민","다현","세진","원준",
        "지영","윤호","태영","수진","승민","채린","하준","가은","민영","현진",
        "서준","예나","유빈","태윤","지수","은채","준하","소율","현성","동윤",
    };

    private static final String[] GRADES = {"사원","대리","과장","차장","부장"};
    private static final int[] GRADE_RATES = {25000, 35000, 50000, 65000, 85000};
    private static final double[] GRADE_DIST = {0.40, 0.25, 0.18, 0.10, 0.07};

    private void seedEmployees() {
        List<Department> depts = deptRepo.findAll();
        int[] perDept = {
            110, 120, 130, 95, 85, 100, 90, 115,
            105, 95, 80, 75, 100, 110, 120, 105,
            90, 85, 80, 110,
        };
        List<Employee> batch = new ArrayList<>(BATCH);
        int seq = 1;
        int nameIdx = 0;
        for (int di = 0; di < depts.size() && di < perDept.length; di++) {
            Department dept = depts.get(di);
            for (int i = 0; i < perDept[di]; i++) {
                int gradeIdx = pickGrade();
                batch.add(Employee.builder()
                    .empNo(String.format("E%04d", seq))
                    .name(LAST_NAMES[nameIdx % LAST_NAMES.length]
                        + FIRST_NAMES[nameIdx % FIRST_NAMES.length])
                    .grade(GRADES[gradeIdx])
                    .department(dept)
                    .hourlyRate(BigDecimal.valueOf(GRADE_RATES[gradeIdx] + rng.nextInt(5) * 1000))
                    .build());
                seq++;
                nameIdx++;
                if (batch.size() >= BATCH) { employeeRepo.saveAll(batch); batch.clear(); }
            }
        }
        if (!batch.isEmpty()) employeeRepo.saveAll(batch);
        log.info("Seeded {} employees", seq - 1);
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

    // ───────────────────── Projects (100) ─────────────────────

    private static final String[][] PROJECT_DATA = {
        {"PRJ-001","경영전략 수립","HQ01","2400","16000"},
        {"PRJ-002","중장기 사업계획","HQ01","2000","14000"},
        {"PRJ-003","예산편성 시스템","HQ01","1800","12000"},
        {"PRJ-004","조직혁신 프로젝트","HQ01","1600","11000"},
        {"PRJ-005","성과관리 체계 구축","HQ01","1400","9500"},
        {"PRJ-006","자산배분 모델링","HQ02","2200","15000"},
        {"PRJ-007","대체투자 운용","HQ02","2000","14000"},
        {"PRJ-008","글로벌 펀드 운용","HQ02","1800","13000"},
        {"PRJ-009","채권 포트폴리오 최적화","HQ02","1600","11500"},
        {"PRJ-010","파생상품 전략","HQ02","1500","10500"},
        {"PRJ-011","코어뱅킹 차세대","HQ03","3200","24000"},
        {"PRJ-012","클라우드 마이그레이션","HQ03","2800","20000"},
        {"PRJ-013","데이터 플랫폼 구축","HQ03","2400","18000"},
        {"PRJ-014","보안관제 고도화","HQ03","2000","15000"},
        {"PRJ-015","인프라 자동화","HQ03","1800","13000"},
        {"PRJ-016","DevOps 파이프라인","HQ03","1600","11500"},
        {"PRJ-017","VaR 모델 개선","HQ04","1800","12500"},
        {"PRJ-018","신용리스크 관리","HQ04","1600","11500"},
        {"PRJ-019","시장리스크 한도관리","HQ04","1400","10000"},
        {"PRJ-020","스트레스 테스트","HQ04","1300","9000"},
        {"PRJ-021","운영리스크 프레임워크","HQ04","1200","8500"},
        {"PRJ-022","내부통제 점검","HQ05","1400","10000"},
        {"PRJ-023","자금세탁방지 (AML)","HQ05","1600","12000"},
        {"PRJ-024","법규 변경 대응","HQ05","1200","8500"},
        {"PRJ-025","컴플라이언스 교육","HQ05","1000","7000"},
        {"PRJ-026","연결재무제표 자동화","HQ06","1600","11500"},
        {"PRJ-027","세무 리스크 분석","HQ06","1400","10000"},
        {"PRJ-028","내부회계관리제도 고도화","HQ06","1800","13000"},
        {"PRJ-029","원가관리 시스템 구축","HQ06","2000","14500"},
        {"PRJ-030","전자세금계산서 자동화","HQ06","1200","8500"},
        {"PRJ-031","인사평가 시스템 개편","HQ07","1400","10000"},
        {"PRJ-032","복리후생 플랫폼","HQ07","1200","8500"},
        {"PRJ-033","사옥 리모델링","HQ07","1000","7000"},
        {"PRJ-034","채용 프로세스 혁신","HQ07","1100","8000"},
        {"PRJ-035","근태관리 시스템","HQ07","900","6500"},
        {"PRJ-036","AI 챗봇 도입","HQ08","2200","16000"},
        {"PRJ-037","RPA 업무 자동화","HQ08","1800","13000"},
        {"PRJ-038","디지털 고객경험 개선","HQ08","1600","11500"},
        {"PRJ-039","블록체인 결제 PoC","HQ08","1400","10000"},
        {"PRJ-040","메타버스 지점 PoC","HQ08","1200","8500"},
        {"PRJ-041","CRM 고도화","HQ09","2000","14500"},
        {"PRJ-042","영업채널 다각화","HQ09","1600","11500"},
        {"PRJ-043","고객 세그먼트 분석","HQ09","1400","10000"},
        {"PRJ-044","영업실적 대시보드","HQ09","1200","8500"},
        {"PRJ-045","신상품 판매전략","HQ09","1100","8000"},
        {"PRJ-046","브랜드 리뉴얼","HQ10","1600","11500"},
        {"PRJ-047","디지털 마케팅 캠페인","HQ10","1400","10000"},
        {"PRJ-048","고객 로열티 프로그램","HQ10","1200","8500"},
        {"PRJ-049","시장조사 프로젝트","HQ10","1000","7000"},
        {"PRJ-050","SNS 콘텐츠 전략","HQ10","900","6500"},
        {"PRJ-051","계약서 관리 시스템","HQ11","1400","10000"},
        {"PRJ-052","소송 리스크 분석","HQ11","1200","8500"},
        {"PRJ-053","지적재산권 관리","HQ11","1000","7000"},
        {"PRJ-054","규제 샌드박스 대응","HQ11","1100","8000"},
        {"PRJ-055","내부감사 시스템 구축","HQ12","1600","11500"},
        {"PRJ-056","감사 자동화 도구","HQ12","1200","8500"},
        {"PRJ-057","부정거래 탐지","HQ12","1400","10000"},
        {"PRJ-058","감사보고서 표준화","HQ12","900","6500"},
        {"PRJ-059","해외법인 관리 체계","HQ13","1800","13000"},
        {"PRJ-060","글로벌 결제 시스템","HQ13","2000","14500"},
        {"PRJ-061","외환거래 플랫폼","HQ13","1600","11500"},
        {"PRJ-062","해외투자 분석","HQ13","1400","10000"},
        {"PRJ-063","글로벌 컴플라이언스","HQ13","1200","8500"},
        {"PRJ-064","콜센터 AI 전환","HQ14","2200","16000"},
        {"PRJ-065","고객 불만 관리","HQ14","1400","10000"},
        {"PRJ-066","옴니채널 서비스","HQ14","1800","13000"},
        {"PRJ-067","VOC 분석 시스템","HQ14","1200","8500"},
        {"PRJ-068","서비스 품질 관리","HQ14","1000","7000"},
        {"PRJ-069","차세대 금융모델 연구","HQ15","2400","18000"},
        {"PRJ-070","퀀트 트레이딩 엔진","HQ15","2200","16000"},
        {"PRJ-071","AI 신용평가 모델","HQ15","2000","14500"},
        {"PRJ-072","자연어처리 리서치","HQ15","1800","13000"},
        {"PRJ-073","강화학습 포트폴리오","HQ15","1600","11500"},
        {"PRJ-074","빅데이터 인프라","HQ16","2200","16000"},
        {"PRJ-075","실시간 분석 파이프라인","HQ16","1800","13000"},
        {"PRJ-076","고객행동 분석","HQ16","1600","11500"},
        {"PRJ-077","이상거래 탐지 ML","HQ16","1400","10000"},
        {"PRJ-078","데이터 거버넌스","HQ16","1200","8500"},
        {"PRJ-079","핀테크 파트너십","HQ17","1600","11500"},
        {"PRJ-080","인슈어테크 PoC","HQ17","1400","10000"},
        {"PRJ-081","디지털 자산 서비스","HQ17","1800","13000"},
        {"PRJ-082","오픈뱅킹 확장","HQ17","1200","8500"},
        {"PRJ-083","유동성 관리 고도화","HQ18","1600","11500"},
        {"PRJ-084","ALM 시스템 구축","HQ18","1800","13000"},
        {"PRJ-085","자금이체 자동화","HQ18","1400","10000"},
        {"PRJ-086","금리리스크 관리","HQ18","1200","8500"},
        {"PRJ-087","부동산 투자분석","HQ19","1600","11500"},
        {"PRJ-088","리츠 운용 시스템","HQ19","1400","10000"},
        {"PRJ-089","자산가치 평가 모델","HQ19","1200","8500"},
        {"PRJ-090","부동산 포트폴리오","HQ19","1100","8000"},
        {"PRJ-091","ESG 경영 대응","HQ20","1800","13000"},
        {"PRJ-092","탄소중립 로드맵","HQ20","1600","11500"},
        {"PRJ-093","ESG 보고서 자동화","HQ20","1400","10000"},
        {"PRJ-094","사회적 가치 측정","HQ20","1200","8500"},
        {"PRJ-095","녹색금융 상품개발","HQ20","1100","8000"},
        {"PRJ-096","전사 ERP 업그레이드","HQ03","3000","22000"},
        {"PRJ-097","차세대 데이터센터","HQ03","2800","20000"},
        {"PRJ-098","전사 보안강화 프로젝트","HQ04","2000","14500"},
        {"PRJ-099","디지털 전환 전략","HQ08","2400","18000"},
        {"PRJ-100","고객360 통합플랫폼","HQ16","2600","19000"},
    };

    private void seedProjects() {
        Map<String, Department> deptMap = deptRepo.findAll().stream()
            .collect(Collectors.toMap(Department::getCode, d -> d));

        Random pr = new Random(77);
        List<Project> batch = new ArrayList<>();
        for (String[] p : PROJECT_DATA) {
            Department dept = deptMap.get(p[2]);
            if (dept == null) continue;

            int startMonth = 6 + pr.nextInt(13);
            LocalDate start = LocalDate.of(2024, 1, 1).plusMonths(startMonth - 1).withDayOfMonth(1);
            int duration = 8 + pr.nextInt(16);
            LocalDate end = start.plusMonths(duration).minusDays(1);
            if (end.isBefore(LocalDate.of(2026, 4, 30))) end = LocalDate.of(2026, 4, 30);

            batch.add(Project.builder()
                .code(p[0]).name(p[1]).ownerDepartment(dept)
                .budgetHours(new BigDecimal(p[3]))
                .budgetCost(new BigDecimal(p[4]).multiply(BigDecimal.valueOf(10000)))
                .startDate(start).endDate(end)
                .build());
        }
        projectRepo.saveAll(batch);
    }

    // ───────────────────── Standard Rates ─────────────────────
    private static final String[] MONTHS = {
        "2025-01","2025-02","2025-03","2025-04","2025-05","2025-06",
        "2025-07","2025-08","2025-09","2025-10","2025-11","2025-12",
        "2026-01","2026-02","2026-03","2026-04",
    };

    private void seedStandardRates() {
        int[][] rateTable = {
            {23000,32000,47000,62000,80000},{23000,32000,47000,62000,80000},{23000,32000,47000,62000,80000},
            {23500,32500,47500,62500,81000},{23500,32500,47500,62500,81000},{23500,32500,47500,62500,81000},
            {24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},
            {24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},{24000,33000,48000,63000,82000},
            {25000,35000,50000,65000,85000},{25000,35000,50000,65000,85000},{25000,35000,50000,65000,85000},{25000,35000,50000,65000,85000},
        };
        List<StandardRate> batch = new ArrayList<>();
        for (int mi = 0; mi < MONTHS.length; mi++) {
            for (int gi = 0; gi < GRADES.length; gi++) {
                batch.add(StandardRate.builder()
                    .yearMonth(MONTHS[mi]).grade(GRADES[gi])
                    .hourlyRate(BigDecimal.valueOf(rateTable[mi][gi]))
                    .build());
            }
        }
        rateRepo.saveAll(batch);
    }

    // ───────────────────── Timesheets ─────────────────────
    private void seedTimesheets() {
        List<Employee> allEmps = employeeRepo.findAll();
        List<Project> allProjs = projectRepo.findAll();
        if (allEmps.isEmpty() || allProjs.isEmpty()) return;

        Map<Long, List<Employee>> empsByDept = allEmps.stream()
            .collect(Collectors.groupingBy(e -> e.getDepartment().getId()));
        Map<Long, List<Project>> projsByDept = allProjs.stream()
            .collect(Collectors.groupingBy(p -> p.getOwnerDepartment().getId()));

        Random tsRng = new Random(123);
        List<Timesheet> batch = new ArrayList<>(BATCH);
        int total = 0;

        for (String ym : MONTHS) {
            int year = Integer.parseInt(ym.substring(0, 4));
            int month = Integer.parseInt(ym.substring(5, 7));

            // Participation grows over time
            double participation;
            if (year == 2025 && month <= 3) participation = 0.25 + tsRng.nextDouble() * 0.05;
            else if (year == 2025 && month <= 6) participation = 0.30 + tsRng.nextDouble() * 0.05;
            else if (year == 2025) participation = 0.35 + tsRng.nextDouble() * 0.05;
            else participation = 0.40 + tsRng.nextDouble() * 0.05;

            if ("2026-04".equals(ym)) participation *= 0.6;

            // Collect work days once per month
            List<LocalDate> workDays = new ArrayList<>();
            LocalDate d = LocalDate.of(year, month, 1);
            LocalDate cutoff = LocalDate.of(2026, 4, 21);
            while (d.getMonthValue() == month) {
                if (d.isAfter(cutoff)) break;
                if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                    workDays.add(d);
                d = d.plusDays(1);
            }
            if (workDays.isEmpty()) continue;

            for (Map.Entry<Long, List<Employee>> entry : empsByDept.entrySet()) {
                Long deptId = entry.getKey();
                List<Employee> deptEmps = entry.getValue();
                List<Project> deptProjs = projsByDept.getOrDefault(deptId, Collections.emptyList());

                for (Employee emp : deptEmps) {
                    if (tsRng.nextDouble() > participation) continue;

                    Project mainProj = deptProjs.isEmpty()
                        ? allProjs.get(tsRng.nextInt(allProjs.size()))
                        : deptProjs.get(tsRng.nextInt(deptProjs.size()));

                    int baseHours = switch (emp.getGrade()) {
                        case "사원" -> 140 + tsRng.nextInt(31);
                        case "대리" -> 150 + tsRng.nextInt(26);
                        case "과장" -> 130 + tsRng.nextInt(31);
                        case "차장" -> 120 + tsRng.nextInt(31);
                        case "부장" -> 80 + tsRng.nextInt(41);
                        default -> 120 + tsRng.nextInt(41);
                    };

                    if ("2026-04".equals(ym)) baseHours = (int)(baseHours * 0.65);

                    Timesheet.Status status;
                    if ("2026-04".equals(ym)) {
                        double sr = tsRng.nextDouble();
                        status = sr < 0.50 ? Timesheet.Status.DRAFT
                               : sr < 0.80 ? Timesheet.Status.SUBMITTED
                               : Timesheet.Status.APPROVED;
                    } else if ("2026-03".equals(ym)) {
                        double sr = tsRng.nextDouble();
                        status = sr < 0.10 ? Timesheet.Status.DRAFT
                               : sr < 0.25 ? Timesheet.Status.SUBMITTED
                               : Timesheet.Status.APPROVED;
                    } else {
                        status = Timesheet.Status.APPROVED;
                    }

                    // 1 entry per employee per project per month (monthly total)
                    LocalDate date = workDays.get(workDays.size() / 2); // mid-month
                    Timesheet.TimesheetBuilder tb = Timesheet.builder()
                        .employee(emp).project(mainProj).workDate(date)
                        .hours(BigDecimal.valueOf(baseHours))
                        .memo(pickMemo(tsRng)).status(status);

                    if (status == Timesheet.Status.SUBMITTED || status == Timesheet.Status.APPROVED) {
                        tb.submittedAt(date.atTime(17 + tsRng.nextInt(2), tsRng.nextInt(60)));
                    }
                    if (status == Timesheet.Status.APPROVED) {
                        tb.approvedAt(date.plusDays(1 + tsRng.nextInt(3))
                            .atTime(9 + tsRng.nextInt(3), tsRng.nextInt(60)))
                          .approvedByEmail("manager@noaats.com");
                    }
                    batch.add(tb.build());
                    total++;

                    if (batch.size() >= BATCH) {
                        timesheetRepo.saveAll(batch);
                        batch.clear();
                    }
                }
            }
            log.info("Timesheets seeded through {}, running total: {}", ym, total);
        }
        if (!batch.isEmpty()) timesheetRepo.saveAll(batch);
        log.info("Seeded {} timesheets total", total);
    }

    private static final String[] MEMOS = {
        "설계 검토","코드 리뷰","회의 참석","문서 작성","테스트 수행",
        "요구사항 분석","데이터 분석","모델 개발","시스템 점검","고객 미팅",
        "기술 검토","아키텍처 설계","프로토타입 개발","성능 최적화","보안 점검",
        "배포 작업","운영 지원","교육 진행","보고서 작성","기획서 작성",
        "리서치","PoC 개발","마이그레이션 작업","모니터링","장애 대응",
        "인터페이스 개발","API 연동","DB 설계","UI/UX 개선","법규 검토",
        "품질관리","프로세스 개선","데이터 정제","시스템 통합","보안 감사",
    };

    private String pickMemo(Random r) {
        return MEMOS[r.nextInt(MEMOS.length)];
    }

    // ───────────────────── Cost Items ─────────────────────
    private void seedCostItems() {
        List<Department> depts = deptRepo.findAll();
        String[][] categories = {
            {"사무실 임대료","INDIRECT"},{"공과금","INDIRECT"},{"통신비","INDIRECT"},
            {"소모품비","INDIRECT"},{"교육훈련비","INDIRECT"},{"복리후생비","INDIRECT"},
            {"감가상각비","INDIRECT"},{"보험료","INDIRECT"},
        };
        long[][] amounts = {
            {1200,180,90,60,120,200,300,150},{1400,200,100,70,140,240,350,170},
            {1600,250,150,100,180,280,400,200},{1000,150,80,50,100,180,250,130},
            {900,130,70,45,90,160,220,110},{1100,170,85,55,110,190,280,140},
            {900,130,70,45,90,160,220,110},{1300,200,120,80,150,220,320,160},
            {1100,170,90,60,110,200,280,140},{1000,150,80,55,100,180,250,130},
            {800,120,65,40,80,150,200,100},{750,110,60,38,75,140,190,95},
            {1100,170,90,60,110,200,280,140},{1200,180,95,65,120,210,290,150},
            {1300,200,110,75,140,230,320,160},{1200,185,100,70,130,210,300,150},
            {950,140,75,48,95,170,230,115},{900,130,70,45,90,160,220,110},
            {850,125,68,42,85,155,210,105},{1100,170,90,60,110,200,280,140},
        };
        Random ciRng = new Random(55);
        List<CostItem> batch = new ArrayList<>(BATCH);
        for (String ym : MONTHS) {
            for (int di = 0; di < depts.size() && di < amounts.length; di++) {
                Department dept = depts.get(di);
                for (int ci = 0; ci < categories.length; ci++) {
                    double variation = 0.90 + ciRng.nextDouble() * 0.20;
                    batch.add(CostItem.builder()
                        .yearMonth(ym).type(CostItem.CostType.INDIRECT).department(dept)
                        .category(categories[ci][0])
                        .amount(BigDecimal.valueOf((long)(amounts[di][ci] * 10000 * variation)))
                        .build());
                }
            }
        }
        costItemRepo.saveAll(batch);
    }

    // ───────────────────── Allocations ─────────────────────
    private void seedAllocations() {
        List<Department> depts = deptRepo.findAll();
        List<Project> projs = projectRepo.findAll();
        if (depts.isEmpty() || projs.isEmpty()) return;

        Random alRng = new Random(88);
        CostAllocation.AllocationBasis[] bases = CostAllocation.AllocationBasis.values();
        List<CostAllocation> batch = new ArrayList<>(BATCH);

        String[] transferMemos = {
            "IT 개발지원","경영기획 자문","리스크 분석 지원","컴플라이언스 검토",
            "데이터 분석 지원","시스템 운영 지원","보안 점검 지원","교육 지원",
            "영업 지원","마케팅 자문","법무 검토","감사 지원",
            "글로벌 사업 지원","고객서비스 지원","R&D 협력","ESG 자문",
        };

        for (String ym : MONTHS) {
            for (Department dept : depts) {
                List<Project> deptProjs = projs.stream()
                    .filter(p -> p.getOwnerDepartment().getId().equals(dept.getId()))
                    .collect(Collectors.toList());
                if (deptProjs.isEmpty()) continue;

                int allocCount = 2 + alRng.nextInt(3);
                for (int i = 0; i < allocCount && i < deptProjs.size(); i++) {
                    batch.add(CostAllocation.builder()
                        .yearMonth(ym).sourceDepartment(dept).targetProject(deptProjs.get(i))
                        .basis(bases[alRng.nextInt(bases.length)])
                        .amount(BigDecimal.valueOf(5_000_000L + alRng.nextInt(20_000_000)))
                        .kind(CostAllocation.AllocationKind.STANDARD_ALLOC)
                        .createdAt(LocalDate.parse(ym + "-15").atTime(10, 0))
                        .build());
                }
            }

            int transferCount = 3 + alRng.nextInt(3);
            for (int t = 0; t < transferCount; t++) {
                Department src = depts.get(alRng.nextInt(depts.size()));
                Department tgt;
                do { tgt = depts.get(alRng.nextInt(depts.size())); } while (tgt.getId().equals(src.getId()));

                int hours = 20 + alRng.nextInt(80);
                int rate = 50000 + alRng.nextInt(40000);

                batch.add(CostAllocation.builder()
                    .yearMonth(ym).sourceDepartment(src).targetDepartment(tgt)
                    .basis(CostAllocation.AllocationBasis.HOURS)
                    .amount(BigDecimal.valueOf((long) hours * rate))
                    .kind(CostAllocation.AllocationKind.TRANSFER)
                    .memo(transferMemos[alRng.nextInt(transferMemos.length)]
                        + " (" + hours + "h × " + String.format("%,d", rate) + "원)")
                    .createdAt(LocalDate.parse(ym + "-20").atTime(14, 0))
                    .build());
            }
        }
        allocationRepo.saveAll(batch);
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
        List<AuditLog> batch = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            int monthOffset = auRng.nextInt(16);
            LocalDate base = LocalDate.of(2025, 1, 1).plusMonths(monthOffset);
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

            batch.add(AuditLog.builder()
                .actor(actors[auRng.nextInt(actors.length)])
                .action(actions[actionIdx])
                .entity(entities[actionIdx])
                .entityId((long)(1 + auRng.nextInt(2000)))
                .detail(detail)
                .createdAt(ts)
                .build());
        }
        auditLogRepo.saveAll(batch);
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
