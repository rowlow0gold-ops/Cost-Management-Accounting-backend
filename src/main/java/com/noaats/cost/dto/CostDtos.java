package com.noaats.cost.dto;

import com.noaats.cost.domain.CostAllocation.AllocationBasis;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CostDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CostAggregateRow {
        private String level;
        private Long keyId;
        private String keyCode;
        private String keyName;
        private BigDecimal hours;
        private BigDecimal directCost;
    }

    @Getter @Setter
    public static class AllocateRequest {
        private String yearMonth;
        private AllocationBasis basis;
    }

    @Getter @Setter
    public static class TransferRequest {
        private String yearMonth;
        private Long sourceDepartmentId;
        private Long targetDepartmentId;
        private BigDecimal hours;
        private BigDecimal hourlyRate;
        private String memo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VarianceRow {
        private Long projectId;
        private String projectCode;
        private String projectName;
        private BigDecimal budgetHours;
        private BigDecimal actualHours;
        private BigDecimal budgetCost;
        private BigDecimal actualCost;
        private BigDecimal hourVariance;
        private BigDecimal costVariance;
        private BigDecimal costVariancePct;

        // Factor analysis (요인 분석)
        /** 표준(예산) 시간당 단가 */
        private BigDecimal stdRate;
        /** 실적 시간당 단가 (실적원가 / 실적공수) */
        private BigDecimal actualRate;
        /** 가격차이 = (실적단가 - 표준단가) × 실적공수 */
        private BigDecimal priceVariance;
        /** 수량차이 = (실적공수 - 예산공수) × 표준단가 */
        private BigDecimal quantityVariance;
    }

    @Getter @Setter
    public static class TimesheetRequest {
        private Long employeeId;
        private Long projectId;
        private LocalDate workDate;
        private BigDecimal hours;
        private String memo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TimeSeriesPoint {
        private String bucket;         // 2026-01 or 2026-01-15
        private String label;          // "1월" / "1일" / ...
        private BigDecimal actualCost;
        private BigDecimal budgetCost;
        private BigDecimal costVariance;
        private BigDecimal costVariancePct;
    }
}
