package com.noaats.cost.service;

import com.noaats.cost.domain.Timesheet;
import com.noaats.cost.dto.CostDtos.TimesheetRequest;
import com.noaats.cost.repository.EmployeeRepository;
import com.noaats.cost.repository.ProjectRepository;
import com.noaats.cost.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectRepository projectRepo;
    private final AuditService audit;

    @Transactional
    public Timesheet create(TimesheetRequest req) {
        // --- server-side validation ---
        if (req.getEmployeeId() == null) throw new IllegalArgumentException("직원을 선택하세요");
        if (req.getProjectId()  == null) throw new IllegalArgumentException("프로젝트를 선택하세요");
        if (req.getWorkDate()   == null) throw new IllegalArgumentException("근무일을 입력하세요");
        if (req.getWorkDate().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("미래 날짜는 입력할 수 없습니다");
        if (req.getHours() == null || req.getHours().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("공수는 0보다 커야 합니다");
        if (req.getHours().compareTo(BigDecimal.valueOf(24)) > 0)
            throw new IllegalArgumentException("하루 최대 24시간까지 입력 가능합니다");

        Timesheet t = Timesheet.builder()
            .employee(employeeRepo.findById(req.getEmployeeId()).orElseThrow())
            .project(projectRepo.findById(req.getProjectId()).orElseThrow())
            .workDate(req.getWorkDate())
            .hours(req.getHours())
            .memo(req.getMemo())
            .status(Timesheet.Status.DRAFT)
            .build();
        Timesheet saved = timesheetRepo.save(t);
        audit.log("CREATE_TIMESHEET", "TIMESHEET", saved.getId(),
                  "emp=" + saved.getEmployee().getEmpNo() + " proj=" + saved.getProject().getCode() +
                  " hours=" + saved.getHours());
        return saved;
    }

    @Transactional
    public Timesheet submit(Long id) {
        Timesheet t = timesheetRepo.findById(id).orElseThrow();
        if (t.getStatus() != Timesheet.Status.DRAFT)
            throw new IllegalStateException("Only DRAFT can be submitted");
        t.setStatus(Timesheet.Status.SUBMITTED);
        t.setSubmittedAt(LocalDateTime.now());
        audit.log("SUBMIT_TIMESHEET", "TIMESHEET", id, null);
        return t;
    }

    @Transactional
    public Timesheet approve(Long id, String approverEmail) {
        Timesheet t = timesheetRepo.findById(id).orElseThrow();
        if (t.getStatus() != Timesheet.Status.SUBMITTED)
            throw new IllegalStateException("Only SUBMITTED can be approved");
        t.setStatus(Timesheet.Status.APPROVED);
        t.setApprovedAt(LocalDateTime.now());
        t.setApprovedByEmail(approverEmail);
        audit.log("APPROVE_TIMESHEET", "TIMESHEET", id, "by=" + approverEmail);
        return t;
    }

    @Transactional
    public Timesheet reject(Long id, String approverEmail) {
        Timesheet t = timesheetRepo.findById(id).orElseThrow();
        if (t.getStatus() != Timesheet.Status.SUBMITTED)
            throw new IllegalStateException("Only SUBMITTED can be rejected");
        t.setStatus(Timesheet.Status.REJECTED);
        t.setApprovedByEmail(approverEmail);
        audit.log("REJECT_TIMESHEET", "TIMESHEET", id, "by=" + approverEmail);
        return t;
    }

    @Transactional
    public void delete(Long id) {
        timesheetRepo.deleteById(id);
        audit.log("DELETE_TIMESHEET", "TIMESHEET", id, null);
    }
}
