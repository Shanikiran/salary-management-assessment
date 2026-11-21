package com.acme.salary.service;

import com.acme.salary.model.SalaryRecord;
import com.acme.salary.repo.SalaryRepository;
import com.acme.salary.validation.SalaryValidator;
import com.acme.salary.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Salary business logic. The one rule that matters most: history is
 * append-only. A revision inserts a new row; it never edits an old one.
 */
@Service
public class SalaryService {

    private final SalaryRepository salaries;
    private final EmployeeService employees;

    public SalaryService(SalaryRepository salaries, EmployeeService employees) {
        this.salaries = salaries;
        this.employees = employees;
    }

    public List<SalaryRecord> history(String employeeId) {
        employees.get(employeeId); // 404 if unknown
        return salaries.history(employeeId);
    }

    public SalaryRecord current(String employeeId) {
        employees.get(employeeId);
        return salaries.current(employeeId, LocalDate.now());
    }

    @Transactional
    public SalaryRecord addRevision(String employeeId, BigDecimal basePay, String currency,
            SalaryRecord.PayFrequency frequency, LocalDate effectiveFrom) {
        employees.get(employeeId); // 404 if unknown
        List<SalaryRecord> existing = salaries.history(employeeId);
        List<String> errors = SalaryValidator.validateRevision(
                basePay, currency == null ? null : currency.trim().toUpperCase(),
                frequency, effectiveFrom, existing);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        SalaryRecord record = new SalaryRecord(null, employeeId, basePay,
                currency.trim().toUpperCase(), frequency, effectiveFrom, LocalDate.now());
        salaries.insert(record);
        // Re-read so the caller gets the generated id.
        return salaries.history(employeeId).stream()
                .filter(r -> r.effectiveFrom().equals(effectiveFrom))
                .findFirst()
                .orElse(record);
    }
}
