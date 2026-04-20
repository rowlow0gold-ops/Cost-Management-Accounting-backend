package com.noaats.cost.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String actor;     // user email

    @Column(nullable = false, length = 50)
    private String action;    // CREATE_TIMESHEET / APPROVE / ALLOCATE / TRANSFER / ...

    @Column(length = 50)
    private String entity;    // TIMESHEET / ALLOCATION / ...

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 1000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
