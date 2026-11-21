package com.acme.salary.validation;

import com.acme.salary.model.Employee;
import com.acme.salary.model.SalaryRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

/**
 * Pure validation logic — no Spring, no database — so it is trivially unit
 * testable. Every rule here comes from the requirements doc:
 * no negative amounts, currency required, no overlapping effective periods.
 */
public final class SalaryValidator {

    private SalaryValidator() {}

    /** Validate a new salary revision against the employee's existing history. */
    public static List<String> validateRevision(BigDecimal basePay, String currency,
            SalaryRecord.PayFrequency frequency, LocalDate effectiveFrom,
            List<SalaryRecord> existingHistory) {
        ValidationException.Collector c = new ValidationException.Collector();

        c.add(basePay == null, "basePay is required");
        c.add(basePay != null && basePay.compareTo(BigDecimal.ZERO) <= 0,
                "basePay must be greater than zero (no negative or zero amounts)");
        c.add(currency == null || currency.isBlank(), "currency is required");
        c.add(currency != null && !currency.isBlank() && !isIsoCurrency(currency),
                "currency '" + currency + "' is not a valid ISO 4217 code");
        c.add(frequency == null, "payFrequency is required (MONTHLY or ANNUAL)");
        c.add(effectiveFrom == null, "effectiveFrom is required");
        c.add(effectiveFrom != null && effectiveFrom.isAfter(LocalDate.now().plusYears(1)),
                "effectiveFrom is more than a year in the future");

        if (effectiveFrom != null && existingHistory != null && !existingHistory.isEmpty()) {
            // Overlap rule: periods run from effectiveFrom until the next
            // record's effectiveFrom, so a new date must be strictly after
            // the latest existing one. A date equal to, or between, existing
            // dates would split or duplicate a period.
            LocalDate latest = existingHistory.stream()
                    .map(SalaryRecord::effectiveFrom)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            c.add(latest != null && !effectiveFrom.isAfter(latest),
                    "effectiveFrom " + effectiveFrom + " must be after the latest existing "
                            + "salary record (" + latest + ") — overlapping effective periods "
                            + "are not allowed");
            c.add(existingHistory.stream().anyMatch(r -> r.effectiveFrom().equals(effectiveFrom)),
                    "a salary record already exists with effectiveFrom " + effectiveFrom);
        }
        return collectErrors(c);
    }

    /** Validate an employee payload (create / update). */
    public static List<String> validateEmployee(String name, String department, String role,
            String country, LocalDate joiningDate, String status) {
        ValidationException.Collector c = new ValidationException.Collector();
        c.add(name == null || name.isBlank(), "name is required");
        c.add(department == null || department.isBlank(), "department is required");
        c.add(role == null || role.isBlank(), "role is required");
        c.add(country == null || country.isBlank(), "country is required");
        c.add(joiningDate == null, "joiningDate is required");
        c.add(joiningDate != null && joiningDate.isAfter(LocalDate.now()),
                "joiningDate cannot be in the future");
        c.add(status != null && !status.isBlank() && !isStatus(status),
                "status must be one of ACTIVE, ON_LEAVE, EXITED");
        return collectErrors(c);
    }

    public static boolean isIsoCurrency(String code) {
        try {
            Currency.getInstance(code.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isStatus(String status) {
        try {
            Employee.EmploymentStatus.valueOf(status.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static List<String> collectErrors(ValidationException.Collector c) {
        try {
            c.check();
            return List.of();
        } catch (ValidationException e) {
            return e.errors();
        }
    }
}
