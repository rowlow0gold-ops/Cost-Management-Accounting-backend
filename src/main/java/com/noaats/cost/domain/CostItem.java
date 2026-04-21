package com.noaats.cost.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cost_item",
       indexes = {
           @Index(name = "idx_ci_ym", columnList = "year_month_val"),
           @Index(name = "idx_ci_ym_dept", columnList = "year_month_val, department_id")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CostItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month_val", nullable = false, length = 7)
    private String yearMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CostType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    public enum CostType { DIRECT, INDIRECT }
}
