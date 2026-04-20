package com.noaats.cost.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "standard_rate",
       uniqueConstraints = @UniqueConstraint(columnNames = {"year_month_val", "grade"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StandardRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month_val", nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false, length = 30)
    private String grade;

    @Column(name = "hourly_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal hourlyRate;
}
