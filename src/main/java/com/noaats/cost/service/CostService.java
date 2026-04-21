package com.noaats.cost.service;

import com.noaats.cost.domain.*;
import com.noaats.cost.domain.CostAllocation.AllocationKind;
import com.noaats.cost.dto.CostDtos.*;
import com.noaats.cost.dto.CostDtos.TimeSeriesPoint;
import com.noaats.cost.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CostService {

    private final TimesheetRepository timesheetRepo;
    private final ProjectRepository projectRepo;
    private final DepartmentRepository departmentRepo;
    private final CostItemRepository costItemRepo;
    private final CostAllocationRepository allocationRepo;
    private final AuditService audit;

    /** Only approved timesheets count as actual cost. Uses fetch-join to avoid N+1. */
    private List<Timesheet> approvedTimesheets(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        return timesheetRepo.findApprovedWithJoin(ym.atDay(1), ym.atEndOfMonth());
    }

    /**
     * Approved timesheets filtered by scope. Uses fetch-join queries.
     *   MONTHLY  → selected month only
     *   ANNUAL   → full selected year
     *   ALL_TIME → every approved timesheet
     */
    private List<Timesheet> approvedInScope(String yearMonth, String scope) {
        String s = scope == null ? "MONTHLY" : scope.toUpperCase();
        if ("ALL_TIME".equals(s)) {
            return timesheetRepo.findAllApprovedWithJoin();
        }
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start, end;
        if ("ANNUAL".equals(s)) {
            start = LocalDate.of(ym.getYear(), 1, 1);
            end = LocalDate.of(ym.getYear(), 12, 31);
        } else {
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
        }
        return timesheetRepo.findApprovedWithJoin(start, end);
    }

    public List<CostAggregateRow> aggregate(String yearMonth, String level) {
        return aggregate(yearMonth, level, "MONTHLY", false);
    }

    public List<CostAggregateRow> aggregate(String yearMonth, String level, String scope) {
        return aggregate(yearMonth, level, scope, false);
    }

    /**
     * @param breakdownByMonth when true, each row is keyed by (levelKey, YYYY-MM) so that
     *        2025-12 and 2026-01 contributions to the same project/department/employee
     *        appear as separate rows. Useful for pie charts that want temporal context.
     */
    public List<CostAggregateRow> aggregate(String yearMonth, String level, String scope,
                                            boolean breakdownByMonth) {
        List<Timesheet> rows = approvedInScope(yearMonth, scope);

        // String key lets us compose "id|YYYY-MM" when breakdownByMonth is true.
        Map<String, BigDecimal[]> bucketStr = new LinkedHashMap<>();
        Map<String, String[]> metaStr = new HashMap<>();
        Map<String, Long> idByKey = new HashMap<>();

        for (Timesheet t : rows) {
            BigDecimal cost = t.getHours().multiply(t.getEmployee().getHourlyRate());
            Long id; String code, name;
            switch (level.toUpperCase()) {
                case "EMPLOYEE" -> {
                    id = t.getEmployee().getId();
                    code = t.getEmployee().getEmpNo();
                    name = t.getEmployee().getName();
                }
                case "PROJECT" -> {
                    id = t.getProject().getId();
                    code = t.getProject().getCode();
                    name = t.getProject().getName();
                }
                case "DEPARTMENT" -> {
                    id = t.getEmployee().getDepartment().getId();
                    code = t.getEmployee().getDepartment().getCode();
                    name = t.getEmployee().getDepartment().getName();
                }
                case "COMPANY" -> { id = 0L; code = "ALL"; name = "전사"; }
                default -> throw new IllegalArgumentException("Unknown level: " + level);
            }
            String ym = breakdownByMonth
                    ? String.format("%04d-%02d", t.getWorkDate().getYear(), t.getWorkDate().getMonthValue())
                    : null;
            String key = breakdownByMonth ? (id + "|" + ym) : String.valueOf(id);
            bucketStr.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            bucketStr.get(key)[0] = bucketStr.get(key)[0].add(t.getHours());
            bucketStr.get(key)[1] = bucketStr.get(key)[1].add(cost);
            idByKey.putIfAbsent(key, id);
            String displayCode = breakdownByMonth ? (code + " · " + ym) : code;
            String displayName = breakdownByMonth ? (name + " · " + ym) : name;
            metaStr.putIfAbsent(key, new String[]{displayCode, displayName});
        }

        return bucketStr.entrySet().stream().map(e ->
            CostAggregateRow.builder()
                .level(level.toUpperCase())
                .keyId(idByKey.get(e.getKey()))
                .keyCode(metaStr.get(e.getKey())[0])
                .keyName(metaStr.get(e.getKey())[1])
                .hours(e.getValue()[0])
                .directCost(e.getValue()[1])
                .build()
        ).sorted(Comparator.comparing(CostAggregateRow::getKeyCode)).toList();
    }

    @Transactional
    public List<CostAllocation> allocate(AllocateRequest req) {
        allocationRepo.deleteByYearMonthAndKind(req.getYearMonth(), AllocationKind.STANDARD_ALLOC);

        BigDecimal totalIndirect = costItemRepo
            .findByYearMonthAndType(req.getYearMonth(), CostItem.CostType.INDIRECT)
            .stream().map(CostItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalIndirect.signum() == 0) return List.of();

        Map<Long, BigDecimal> weight = new HashMap<>();
        switch (req.getBasis()) {
            case HOURS -> aggregate(req.getYearMonth(), "PROJECT")
                .forEach(r -> weight.put(r.getKeyId(), r.getHours()));
            case HEADCOUNT -> {
                Map<Long, Set<Long>> empByProject = new HashMap<>();
                for (Timesheet t : approvedTimesheets(req.getYearMonth())) {
                    empByProject.computeIfAbsent(t.getProject().getId(), k -> new HashSet<>())
                                .add(t.getEmployee().getId());
                }
                empByProject.forEach((pid, set) -> weight.put(pid, BigDecimal.valueOf(set.size())));
            }
            case REVENUE -> projectRepo.findAll().forEach(p ->
                weight.put(p.getId(), p.getBudgetCost() == null ? BigDecimal.ZERO : p.getBudgetCost()));
        }

        BigDecimal totalWeight = weight.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() == 0) return List.of();

        List<CostAllocation> saved = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : weight.entrySet()) {
            BigDecimal share = e.getValue()
                .divide(totalWeight, 6, RoundingMode.HALF_UP)
                .multiply(totalIndirect)
                .setScale(2, RoundingMode.HALF_UP);
            if (share.signum() == 0) continue;

            Project p = projectRepo.findById(e.getKey()).orElseThrow();
            saved.add(allocationRepo.save(CostAllocation.builder()
                .yearMonth(req.getYearMonth())
                .sourceDepartment(p.getOwnerDepartment())
                .targetProject(p)
                .basis(req.getBasis())
                .amount(share)
                .kind(AllocationKind.STANDARD_ALLOC)
                .createdAt(LocalDateTime.now())
                .build()));
        }
        audit.log("ALLOCATE", "ALLOCATION", null,
                  "month=" + req.getYearMonth() + " basis=" + req.getBasis() +
                  " total=" + totalIndirect);
        return saved;
    }

    public List<CostAllocation> transfers(String yearMonth) {
        return allocationRepo.findByYearMonthAndKind(yearMonth, CostAllocation.AllocationKind.TRANSFER);
    }

    @Transactional
    public CostAllocation transfer(TransferRequest req) {
        if (req.getSourceDepartmentId() == null || req.getTargetDepartmentId() == null)
            throw new IllegalArgumentException("제공/수혜 본부를 모두 선택하세요");
        if (req.getSourceDepartmentId().equals(req.getTargetDepartmentId()))
            throw new IllegalArgumentException("제공 본부와 수혜 본부는 달라야 합니다");
        if (req.getHours() == null || req.getHours().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("공수는 0보다 커야 합니다");
        if (req.getHourlyRate() == null || req.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("단가는 0보다 커야 합니다");

        BigDecimal amount = req.getHours().multiply(req.getHourlyRate())
                                          .setScale(2, RoundingMode.HALF_UP);
        Department src = departmentRepo.findById(req.getSourceDepartmentId()).orElseThrow();
        Department tgt = departmentRepo.findById(req.getTargetDepartmentId()).orElseThrow();
        CostAllocation saved = allocationRepo.save(CostAllocation.builder()
            .yearMonth(req.getYearMonth())
            .sourceDepartment(src)
            .targetDepartment(tgt)
            .basis(CostAllocation.AllocationBasis.HOURS)
            .amount(amount)
            .kind(AllocationKind.TRANSFER)
            .memo(req.getMemo())
            .createdAt(LocalDateTime.now())
            .build());
        audit.log("TRANSFER", "ALLOCATION", saved.getId(),
                  "from=" + src.getCode() + " to=" + tgt.getCode() + " amount=" + amount);
        return saved;
    }

    public List<VarianceRow> variance(String yearMonth) {
        return variance(yearMonth, "MONTHLY");
    }

    /**
     * scope:
     *   MONTHLY  → budget = annualBudget / 12, actual = selected month only
     *   ANNUAL   → budget = annualBudget,      actual = YTD (year-start ~ selected month)
     *   ALL_TIME → budget = annualBudget,      actual = ALL approved timesheets ever
     */
    public List<VarianceRow> variance(String yearMonth, String scope) {
        String scopeU = scope == null ? "MONTHLY" : scope.toUpperCase();

        // Build actuals per project using the scope-aware helper (both MONTHLY and ANNUAL
        // now cover the full selected year; ALL_TIME covers everything).
        Map<Long, BigDecimal[]> actualsByProj = new HashMap<>();
        for (Timesheet t : approvedInScope(yearMonth, scopeU)) {
            BigDecimal cost = t.getHours().multiply(t.getEmployee().getHourlyRate());
            Long pid = t.getProject().getId();
            actualsByProj.computeIfAbsent(pid,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] agg = actualsByProj.get(pid);
            agg[0] = agg[0].add(t.getHours());
            agg[1] = agg[1].add(cost);
        }

        // Indirect allocations per project in the same scope.
        Map<Long, BigDecimal> indirectByProject = new HashMap<>();
        if ("ALL_TIME".equals(scopeU)) {
            allocationRepo.findAll().forEach(a -> {
                if (a.getKind() == AllocationKind.STANDARD_ALLOC && a.getTargetProject() != null)
                    indirectByProject.merge(a.getTargetProject().getId(),
                            a.getAmount(), BigDecimal::add);
            });
        } else if ("ANNUAL".equals(scopeU)) {
            YearMonth ym = YearMonth.parse(yearMonth);
            for (int m = 1; m <= 12; m++) {
                String ymStr = String.format("%04d-%02d", ym.getYear(), m);
                allocationRepo.findByYearMonthAndKind(ymStr, AllocationKind.STANDARD_ALLOC)
                    .forEach(a -> {
                        if (a.getTargetProject() != null)
                            indirectByProject.merge(a.getTargetProject().getId(),
                                    a.getAmount(), BigDecimal::add);
                    });
            }
        } else { // MONTHLY
            allocationRepo.findByYearMonthAndKind(yearMonth, AllocationKind.STANDARD_ALLOC)
                .forEach(a -> {
                    if (a.getTargetProject() != null)
                        indirectByProject.merge(a.getTargetProject().getId(),
                                a.getAmount(), BigDecimal::add);
                });
        }

        List<VarianceRow> out = new ArrayList<>();

        // Monthly → annual budget / 12, Annual & All-time → full annual budget.
        BigDecimal divisor = "MONTHLY".equals(scopeU)
                ? BigDecimal.valueOf(12)
                : BigDecimal.ONE;

        // In MONTHLY scope, only show projects that actually had activity that month.
        List<Project> projects = "MONTHLY".equals(scopeU)
                ? projectRepo.findAll().stream()
                        .filter(p -> actualsByProj.containsKey(p.getId())
                                  || indirectByProject.containsKey(p.getId()))
                        .toList()
                : projectRepo.findAll();

        for (Project p : projects) {
            BigDecimal[] agg = actualsByProj.getOrDefault(p.getId(),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal actualHours = agg[0];
            BigDecimal direct = agg[1];
            BigDecimal indirect = indirectByProject.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal actualCost = direct.add(indirect);

            BigDecimal budgetH = nz(p.getBudgetHours()).divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal budgetC = nz(p.getBudgetCost()).divide(divisor, 2, RoundingMode.HALF_UP);

            BigDecimal costVar = actualCost.subtract(budgetC);
            BigDecimal pct = budgetC.signum() == 0 ? BigDecimal.ZERO
                : costVar.multiply(BigDecimal.valueOf(100))
                         .divide(budgetC, 2, RoundingMode.HALF_UP);

            // === Factor analysis (요인 분석) ===
            BigDecimal stdRate = budgetH.signum() == 0 ? BigDecimal.ZERO
                : budgetC.divide(budgetH, 2, RoundingMode.HALF_UP);
            BigDecimal actualRate = actualHours.signum() == 0 ? BigDecimal.ZERO
                : direct.divide(actualHours, 2, RoundingMode.HALF_UP);
            // Price variance uses DIRECT cost only (rate × hours).
            BigDecimal priceVariance = actualRate.subtract(stdRate)
                    .multiply(actualHours).setScale(2, RoundingMode.HALF_UP);
            BigDecimal quantityVariance = actualHours.subtract(budgetH)
                    .multiply(stdRate).setScale(2, RoundingMode.HALF_UP);

            out.add(VarianceRow.builder()
                .projectId(p.getId())
                .projectCode(p.getCode())
                .projectName(p.getName())
                .budgetHours(budgetH)
                .actualHours(actualHours)
                .budgetCost(budgetC)
                .actualCost(actualCost)
                .hourVariance(actualHours.subtract(budgetH))
                .costVariance(costVar)
                .costVariancePct(pct)
                .stdRate(stdRate)
                .actualRate(actualRate)
                .priceVariance(priceVariance)
                .quantityVariance(quantityVariance)
                .build());
        }
        out.sort(Comparator.comparing(VarianceRow::getProjectCode));
        return out;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /* =========================================================
     * 5) Variance time-series (for trend charts)
     *    MONTHLY  → 1월..12월 of selected year (X = months)
     *    ANNUAL   → yearly totals across years present in data (X = years)
     *    ALL_TIME → monthly points across every year (X = YYYY-MM)
     * ========================================================= */
    public List<TimeSeriesPoint> varianceTimeSeries(String yearMonth, String scope) {
        String s = scope == null ? "MONTHLY" : scope.toUpperCase();

        // Budget denominator: only projects that had APPROVED activity in the
        // relevant window. Including every project (even inactive ones) would
        // crush the variance chart to -100% because budget would dwarf actual.
        java.util.Set<Long> activeProjectIds = new java.util.HashSet<>();
        List<Timesheet> windowTimesheets;
        if ("MONTHLY".equals(s)) {
            YearMonth end = YearMonth.parse(yearMonth);
            YearMonth start = end.minusMonths(11);
            windowTimesheets = timesheetRepo.findApprovedWithJoin(
                    start.atDay(1), end.atEndOfMonth());
        } else if ("ANNUAL".equals(s)) {
            windowTimesheets = approvedInScope(yearMonth, "ANNUAL");
        } else {
            windowTimesheets = timesheetRepo.findAllApprovedWithJoin();
        }
        for (Timesheet t : windowTimesheets) {
            activeProjectIds.add(t.getProject().getId());
        }
        BigDecimal annualBudget = projectRepo.findAll().stream()
                .filter(p -> activeProjectIds.contains(p.getId()))
                .map(p -> nz(p.getBudgetCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TimeSeriesPoint> out = new ArrayList<>();

        if ("MONTHLY".equals(s)) {
            // Rolling window: past 12 months up to and including the selected month.
            YearMonth end = YearMonth.parse(yearMonth);
            YearMonth start = end.minusMonths(11);

            Map<String, BigDecimal> byYm = new TreeMap<>();
            // Reuse the already-loaded windowTimesheets (approved, fetch-joined)
            for (Timesheet t : windowTimesheets) {
                String key = String.format("%04d-%02d",
                        t.getWorkDate().getYear(), t.getWorkDate().getMonthValue());
                BigDecimal c = t.getHours().multiply(t.getEmployee().getHourlyRate());
                byYm.merge(key, c, BigDecimal::add);
            }

            YearMonth cursor = start;
            for (int i = 0; i < 12; i++) {
                String key = String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue());
                BigDecimal actual = byYm.getOrDefault(key, BigDecimal.ZERO);
                // Monthly budget uses quarterly phasing (20/30/30/20) instead of flat /12.
                BigDecimal monthBudget = BudgetPhasing.monthBudget(annualBudget, cursor.getMonthValue());
                BigDecimal var = actual.subtract(monthBudget);
                BigDecimal pct = monthBudget.signum() == 0 ? BigDecimal.ZERO
                        : var.multiply(BigDecimal.valueOf(100)).divide(monthBudget, 2, RoundingMode.HALF_UP);
                out.add(TimeSeriesPoint.builder()
                        .bucket(key)
                        .label(cursor.getYear() + "." + cursor.getMonthValue())
                        .actualCost(actual)
                        .budgetCost(monthBudget)
                        .costVariance(var)
                        .costVariancePct(pct)
                        .build());
                cursor = cursor.plusMonths(1);
            }
            return out;
        }

        if ("ANNUAL".equals(s)) {
            YearMonth ym = YearMonth.parse(yearMonth);
            int year = ym.getYear();
            Map<Integer, BigDecimal> byMonth = new TreeMap<>();
            for (Timesheet t : approvedInScope(yearMonth, "ANNUAL")) {
                BigDecimal c = t.getHours().multiply(t.getEmployee().getHourlyRate());
                byMonth.merge(t.getWorkDate().getMonthValue(), c, BigDecimal::add);
            }
            for (int m = 1; m <= 12; m++) {
                BigDecimal actual = byMonth.getOrDefault(m, BigDecimal.ZERO);
                // Phased budget per month (Q1=20%, Q2/Q3=30%, Q4=20% of annual).
                BigDecimal monthBudget = BudgetPhasing.monthBudget(annualBudget, m);
                BigDecimal var = actual.subtract(monthBudget);
                BigDecimal pct = monthBudget.signum() == 0 ? BigDecimal.ZERO
                        : var.multiply(BigDecimal.valueOf(100)).divide(monthBudget, 2, RoundingMode.HALF_UP);
                out.add(TimeSeriesPoint.builder()
                        .bucket(String.format("%04d-%02d", year, m))
                        .label(m + "월")
                        .actualCost(actual)
                        .budgetCost(monthBudget)
                        .costVariance(var)
                        .costVariancePct(pct)
                        .build());
            }
            return out;
        }

        // ALL_TIME: group by year (yearly totals vs annual budget)
        Map<Integer, BigDecimal> byYear = new TreeMap<>();
        for (Timesheet t : windowTimesheets) {
            BigDecimal c = t.getHours().multiply(t.getEmployee().getHourlyRate());
            byYear.merge(t.getWorkDate().getYear(), c, BigDecimal::add);
        }
        for (Map.Entry<Integer, BigDecimal> e : byYear.entrySet()) {
            int y = e.getKey();
            BigDecimal actual = e.getValue();
            BigDecimal var = actual.subtract(annualBudget);
            BigDecimal pct = annualBudget.signum() == 0 ? BigDecimal.ZERO
                    : var.multiply(BigDecimal.valueOf(100)).divide(annualBudget, 2, RoundingMode.HALF_UP);
            out.add(TimeSeriesPoint.builder()
                    .bucket(String.valueOf(y))
                    .label(y + "년")
                    .actualCost(actual)
                    .budgetCost(annualBudget)
                    .costVariance(var)
                    .costVariancePct(pct)
                    .build());
        }
        return out;
    }
}
