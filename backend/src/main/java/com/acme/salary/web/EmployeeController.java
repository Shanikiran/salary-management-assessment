package com.acme.salary.web;

import com.acme.salary.model.Employee;
import com.acme.salary.model.SalaryRecord;
import com.acme.salary.service.CsvService;
import com.acme.salary.service.EmployeeService;
import com.acme.salary.service.NotFoundException;
import com.acme.salary.service.SalaryService;
import com.acme.salary.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Employee + salary REST API. All dates are ISO YYYY-MM-DD. */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employees;
    private final SalaryService salaries;
    private final CsvService csv;

    public EmployeeController(EmployeeService employees, SalaryService salaries, CsvService csv) {
        this.employees = employees;
        this.salaries = salaries;
        this.csv = csv;
    }

    public record CreateEmployeeRequest(String name, String department, String role,
            String country, String city, LocalDate joiningDate, String status,
            BigDecimal basePay, String currency, String payFrequency,
            LocalDate effectiveFrom) {}
    public record UpdateEmployeeRequest(String name, String department, String role,
            String country, String city, LocalDate joiningDate, String status) {}
    public record SalaryRequest(BigDecimal basePay, String currency,
            String payFrequency, LocalDate effectiveFrom) {}
    public record EmployeeDetail(Employee employee, SalaryRecord currentSalary,
            List<SalaryRecord> salaryHistory) {}

    @GetMapping
    public EmployeeService.Page<Employee> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return employees.search(name, department, country, status, sort, order, page, size);
    }

    @GetMapping("/{id}")
    public EmployeeDetail detail(@PathVariable String id) {
        Employee e = employees.get(id);
        return new EmployeeDetail(e, salaries.current(id), salaries.history(id));
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody CreateEmployeeRequest req) {
        Employee e = employees.create(req.name(), req.department(), req.role(),
                req.country(), req.city(), req.joiningDate(), req.status());
        // Optional initial salary on the same request (validated the same way).
        if (req.basePay() != null || req.currency() != null
                || req.payFrequency() != null || req.effectiveFrom() != null) {
            SalaryRecord.PayFrequency freq = null;
            if (req.payFrequency() != null) {
                try {
                    freq = SalaryRecord.PayFrequency.valueOf(req.payFrequency().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new ValidationException("payFrequency must be MONTHLY or ANNUAL");
                }
            }
            salaries.addRevision(e.id(), req.basePay(), req.currency(), freq, req.effectiveFrom());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(e);
    }

    @PutMapping("/{id}")
    public Employee update(@PathVariable String id, @RequestBody UpdateEmployeeRequest req) {
        return employees.update(id, req.name(), req.department(), req.role(),
                req.country(), req.city(), req.joiningDate(), req.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        employees.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- salary history ----

    @GetMapping("/{id}/salaries")
    public List<SalaryRecord> salaryHistory(@PathVariable String id) {
        return salaries.history(id);
    }

    @GetMapping("/{id}/salaries/current")
    public ResponseEntity<SalaryRecord> currentSalary(@PathVariable String id) {
        SalaryRecord current = salaries.current(id);
        return current == null ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(current);
    }

    @PostMapping("/{id}/salaries")
    public ResponseEntity<SalaryRecord> addSalary(@PathVariable String id,
            @RequestBody SalaryRequest req) {
        SalaryRecord.PayFrequency freq = null;
        if (req.payFrequency() != null) {
            try {
                freq = SalaryRecord.PayFrequency.valueOf(req.payFrequency().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("payFrequency must be MONTHLY or ANNUAL");
            }
        }
        SalaryRecord created = salaries.addRevision(
                id, req.basePay(), req.currency(), freq, req.effectiveFrom());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ---- bulk import ----

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvService.ImportResult importCsv(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) {
            throw new ValidationException("uploaded file is empty");
        }
        return csv.importEmployees(file.getInputStream());
    }

    // ---- error mapping ----

    @org.springframework.web.bind.annotation.ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> validation(ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "validation failed", "details", e.errors()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
