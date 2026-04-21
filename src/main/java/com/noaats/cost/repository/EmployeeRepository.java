package com.noaats.cost.repository;

import com.noaats.cost.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDepartmentId(Long departmentId);

    @Query("SELECT e FROM Employee e LEFT JOIN e.department d WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.empNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.grade) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Employee> search(@Param("keyword") String keyword, Pageable pageable);
}
