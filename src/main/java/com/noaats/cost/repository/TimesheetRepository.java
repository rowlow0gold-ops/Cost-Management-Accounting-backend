package com.noaats.cost.repository;

import com.noaats.cost.domain.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByWorkDateBetween(LocalDate start, LocalDate end);
    List<Timesheet> findByStatus(Timesheet.Status status);
    List<Timesheet> findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status status);
    List<Timesheet> findAllByOrderByWorkDateDescIdDesc();
    List<Timesheet> findByEmployeeId(Long employeeId);

    /** Fetch-joined approved timesheets in date range — avoids N+1 and loads only APPROVED */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.employee JOIN FETCH t.project " +
           "WHERE t.status = 'APPROVED' AND t.workDate BETWEEN :start AND :end")
    List<Timesheet> findApprovedWithJoin(@Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    /** All approved timesheets with fetch joins */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.employee JOIN FETCH t.project " +
           "WHERE t.status = 'APPROVED'")
    List<Timesheet> findAllApprovedWithJoin();

    @Query("SELECT t FROM Timesheet t LEFT JOIN t.employee e LEFT JOIN t.project p WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.empNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.memo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Timesheet> search(@Param("status") Timesheet.Status status,
                           @Param("keyword") String keyword,
                           Pageable pageable);
}
