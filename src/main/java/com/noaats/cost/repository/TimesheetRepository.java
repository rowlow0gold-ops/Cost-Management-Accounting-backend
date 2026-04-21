package com.noaats.cost.repository;

import com.noaats.cost.domain.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByWorkDateBetween(LocalDate start, LocalDate end);
    List<Timesheet> findByStatus(Timesheet.Status status);
    List<Timesheet> findByStatusOrderByWorkDateDescIdDesc(Timesheet.Status status);
    List<Timesheet> findAllByOrderByWorkDateDescIdDesc();
    List<Timesheet> findByEmployeeId(Long employeeId);
}
