package com.noaats.cost.controller;

import com.noaats.cost.domain.*;
import com.noaats.cost.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/masters")
@RequiredArgsConstructor
public class MastersController {

    private final DepartmentRepository deptRepo;
    private final EmployeeRepository empRepo;
    private final ProjectRepository projRepo;
    private final StandardRateRepository rateRepo;
    private final CostItemRepository costItemRepo;

    // ----- Departments
    @GetMapping("/departments")
    public List<Department> deptList() { return deptRepo.findAll(); }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public Department deptCreate(@RequestBody Department d) { return deptRepo.save(d); }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Department deptUpdate(@PathVariable Long id, @RequestBody Department d) {
        d.setId(id); return deptRepo.save(d);
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deptDelete(@PathVariable Long id) { deptRepo.deleteById(id); }

    // ----- Employees
    @GetMapping("/employees")
    public List<Employee> empList() { return empRepo.findAll(); }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public Employee empCreate(@RequestBody Employee e) { return empRepo.save(e); }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Employee empUpdate(@PathVariable Long id, @RequestBody Employee e) {
        e.setId(id); return empRepo.save(e);
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void empDelete(@PathVariable Long id) { empRepo.deleteById(id); }

    // ----- Projects
    @GetMapping("/projects")
    public List<Project> projList() { return projRepo.findAll(); }

    @PostMapping("/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public Project projCreate(@RequestBody Project p) { return projRepo.save(p); }

    @PutMapping("/projects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Project projUpdate(@PathVariable Long id, @RequestBody Project p) {
        p.setId(id); return projRepo.save(p);
    }

    @DeleteMapping("/projects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void projDelete(@PathVariable Long id) { projRepo.deleteById(id); }

    // ----- Standard rates
    @GetMapping("/rates")
    public List<StandardRate> rates(@RequestParam(required = false) String yearMonth) {
        return yearMonth == null ? rateRepo.findAll() : rateRepo.findByYearMonth(yearMonth);
    }

    @PostMapping("/rates")
    @PreAuthorize("hasRole('ADMIN')")
    public StandardRate rateCreate(@RequestBody StandardRate r) { return rateRepo.save(r); }

    // ----- Cost items (indirect costs etc.)
    @GetMapping("/cost-items")
    public List<CostItem> costItems(@RequestParam String yearMonth) {
        return costItemRepo.findByYearMonth(yearMonth);
    }

    @PostMapping("/cost-items")
    @PreAuthorize("hasRole('ADMIN')")
    public CostItem costItemCreate(@RequestBody CostItem c) { return costItemRepo.save(c); }
}
