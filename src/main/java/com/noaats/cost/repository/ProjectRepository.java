package com.noaats.cost.repository;

import com.noaats.cost.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerDepartmentId(Long departmentId);

    @Query("SELECT p FROM Project p LEFT JOIN p.ownerDepartment d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Project> search(@Param("keyword") String keyword, Pageable pageable);
}
