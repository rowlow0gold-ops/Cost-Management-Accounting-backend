package com.noaats.cost.repository;

import com.noaats.cost.domain.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);

    @Query("SELECT d FROM Department d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Department> search(@Param("keyword") String keyword, Pageable pageable);
}
