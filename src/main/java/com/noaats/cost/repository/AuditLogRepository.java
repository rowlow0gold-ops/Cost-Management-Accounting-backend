package com.noaats.cost.repository;

import com.noaats.cost.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(a.actor) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.entity) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.detail) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<AuditLog> search(@Param("keyword") String keyword, Pageable pageable);
}
