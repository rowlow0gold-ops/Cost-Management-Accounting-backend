package com.noaats.cost.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_allocation",
       indexes = @Index(name = "idx_alloc_ym", columnList = "year_month_val"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CostAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month_val", nullable = false, length = 7)
    private String yearMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_department_id", nullable = false)
    private Department sourceDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_project_id")
    private Project targetProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id")
    private Department targetDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationBasis basis;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationKind kind;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum AllocationBasis { HOURS, HEADCOUNT, REVENUE }
    public enum AllocationKind  { STANDARD_ALLOC, TRANSFER }
}
