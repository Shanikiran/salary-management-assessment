package com.acme.salary.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One salary record. History is append-only: a revision is a NEW row with a
 * later effective_from; existing rows are never updated or deleted.
 * A record's pay period runs from its effective_from until the next
 * record's effective_from (open-ended for the latest).
 */
public record SalaryRecord(
        Long id,
        String employeeId,
        BigDecimal basePay,
        String currency,          // ISO 4217, e.g. INR
        PayFrequency payFrequency,
        LocalDate effectiveFrom,
        LocalDate createdAt
) {
    public enum PayFrequency { MONTHLY, ANNUAL }
}
