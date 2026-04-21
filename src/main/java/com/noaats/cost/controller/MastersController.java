package com.noaats.cost.controller;

import com.noaats.cost.domain.*;
import com.noaats.cost.repository.*;
import com.noaats.cost.service.CostItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masters")
@RequiredArgsConstructor
public class MastersController {

    private final DepartmentRepository deptRepo;
    private final EmployeeRepository empRepo;
    private final ProjectRepository projRepo;
    private final StandardRateRepository rateRepo;
    private final CostItemRepository costItemRepo;
    private final CostItemService costItemService;

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

    @PostMapping("/cost-items/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> validateCostItems(@RequestParam("file") MultipartFile file) {
        try {
            int count = costItemService.validateExcel(file);
            return ResponseEntity.ok(Map.of("valid", count, "message", count + "건 검증 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/cost-items/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> importCostItems(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "MERGE") String mode) {
        try {
            int count = costItemService.importFromExcel(file, mode);
            return ResponseEntity.ok(Map.of("imported", count, "message", count + "건이 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
