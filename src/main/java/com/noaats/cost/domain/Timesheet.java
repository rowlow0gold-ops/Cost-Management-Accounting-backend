package com.noaats.cost.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "timesheet",
       indexes = {
           @Index(name = "idx_ts_work_date", columnList = "work_date"),
           @Index(name = "idx_ts_status", columnList = "status"),
           @Index(name = "idx_ts_status_date", columnList = "status, work_date"),
           @Index(name = "idx_ts_emp_proj", columnList = "employee_id, project_id"),
           @Index(name = "idx_ts_emp_status", columnList = "employee_id, status"),
           @Index(name = "idx_ts_proj_status", columnList = "project_id, status")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Timesheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal hours;

    @Column(length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_email", length = 100)
    private String approvedByEmail;

    public enum Status { DRAFT, SUBMITTED, APPROVED, REJECTED }
}
