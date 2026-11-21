package com.acme.salary.model;

import java.time.LocalDate;

/** An employee record. Salary lives in {@link SalaryRecord}, never here. */
public record Employee(
        String id,
        String name,
        String department,
        String role,
        String country,
        String city,
        LocalDate joiningDate,
        EmploymentStatus status,
        LocalDate createdAt,
        LocalDate updatedAt
) {
    public enum EmploymentStatus { ACTIVE, ON_LEAVE, EXITED }
}
