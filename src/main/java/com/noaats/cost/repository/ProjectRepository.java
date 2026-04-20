package com.noaats.cost.repository;

import com.noaats.cost.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerDepartmentId(Long departmentId);
}
