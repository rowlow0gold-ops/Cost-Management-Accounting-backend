package com.noaats.cost.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monthly budget phasing factors.
 *
 * Real-world 관리회계 rarely distributes an annual budget evenly (1/12 per month).
 * Instead, budgets are "phased" by quarter based on business seasonality — for
 * financial SI projects a common pattern is:
 *
 *   Q1 : 20 %   (ramp-up, requirements)
 *   Q2 : 30 %   (development peak)
 *   Q3 : 30 %   (integration, UAT)
 *   Q4 : 20 %   (stabilization, closing)
 *
 * This helper converts annual budget into a month's share using the above pattern.
 */
public final class BudgetPhasing {

    private BudgetPhasing() {}

    private static final BigDecimal[] QUARTER_RATIO = {
        new BigDecimal("0.20"),   // Q1
        new BigDecimal("0.30"),   // Q2
        new BigDecimal("0.30"),   // Q3
        new BigDecimal("0.20"),   // Q4
    };

    /** Budget for a single month, derived from the annual budget + Q-phasing. */
    public static BigDecimal monthBudget(BigDecimal annualBudget, int monthOfYear) {
        int q = (monthOfYear - 1) / 3;                              // 0..3
        BigDecimal q_share = annualBudget.multiply(QUARTER_RATIO[q]); // quarter total
        return q_share.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }

    /** Budget for a specific quarter (1..4). */
    public static BigDecimal quarterBudget(BigDecimal annualBudget, int quarter) {
        return annualBudget.multiply(QUARTER_RATIO[quarter - 1])
                           .setScale(2, RoundingMode.HALF_UP);
    }

    /** Phasing ratio (for UI display). */
    public static BigDecimal quarterRatio(int quarter) {
        return QUARTER_RATIO[quarter - 1];
    }
}
