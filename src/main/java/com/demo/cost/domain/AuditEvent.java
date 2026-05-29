package com.demo.cost.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "ix_audit_email", columnList = "email"),
    @Index(name = "ix_audit_created_at", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String email;

    @Column(nullable = false, length = 50)
    private String action;   // LOGIN_SUCCESS, LOGIN_FAILURE, REGISTER, REFRESH_OK, REFRESH_REPLAY, LOGOUT, ACCOUNT_LOCKED

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
