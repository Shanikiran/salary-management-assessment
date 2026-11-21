package com.acme.salary.service;

import com.acme.salary.model.Employee;
import com.acme.salary.repo.EmployeeRepository;
import com.acme.salary.repo.SalaryRepository;
import com.acme.salary.validation.SalaryValidator;
import com.acme.salary.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Employee business logic: validation, id generation, paged search. */
@Service
public class EmployeeService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employees;
    private final SalaryRepository salaries;

    public EmployeeService(EmployeeRepository employees, SalaryRepository salaries) {
        this.employees = employees;
        this.salaries = salaries;
    }

    public record Page<T>(List<T> content, int page, int size,
                          long totalElements, int totalPages) {}

    public Page<Employee> search(String name, String department, String country,
            String status, String sort, String order, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        long total = employees.count(name, department, country, status);
        List<Employee> content = employees.search(name, department, country, status,
                sort, order, safeSize, safePage * safeSize);
        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new Page<>(content, safePage, safeSize, total, totalPages);
    }

    public Employee get(String id) {
        Employee e = employees.findById(id);
        if (e == null) {
            throw new NotFoundException("employee " + id + " not found");
        }
        return e;
    }

    @Transactional
    public Employee create(String name, String department, String role, String country,
            String city, LocalDate joiningDate, String status) {
        List<String> errors = SalaryValidator.validateEmployee(
                name, department, role, country, joiningDate, status);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        LocalDate today = LocalDate.now();
        Employee e = new Employee(
                employees.nextId(),
                name.trim(), department.trim(), role.trim(), country.trim(),
                city == null ? null : city.trim(),
                joiningDate,
                status == null || status.isBlank()
                        ? Employee.EmploymentStatus.ACTIVE
                        : Employee.EmploymentStatus.valueOf(status.trim().toUpperCase()),
                today, today);
        employees.insert(e);
        return e;
    }

    @Transactional
    public Employee update(String id, String name, String department, String role,
            String country, String city, LocalDate joiningDate, String status) {
        Employee existing = get(id);
        List<String> errors = SalaryValidator.validateEmployee(
                name, department, role, country, joiningDate, status);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Employee updated = new Employee(
                existing.id(),
                name.trim(), department.trim(), role.trim(), country.trim(),
                city == null ? null : city.trim(),
                joiningDate,
                status == null || status.isBlank()
                        ? existing.status()
                        : Employee.EmploymentStatus.valueOf(status.trim().toUpperCase()),
                existing.createdAt(), LocalDate.now());
        employees.update(updated);
        return updated;
    }

    /** Deletes the employee and their salary history (history has no meaning without the employee). */
    @Transactional
    public void delete(String id) {
        get(id);
        salaries.deleteForEmployee(id);
        employees.delete(id);
    }

    public long totalCount() {
        return employees.totalCount();
    }
}
