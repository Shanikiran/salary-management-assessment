package com.acme.salary.service;

import com.acme.salary.model.Employee;
import com.acme.salary.model.SalaryRecord;
import com.acme.salary.validation.SalaryValidator;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV bulk import (the realistic migration path off Excel) and CSV export
 * of insight tables. Import is row-by-row: one bad row never blocks the
 * rest, and every failure is reported with its row number and reason.
 */
@Service
public class CsvService {

    public record ImportError(int row, String reason) {}
    public record ImportResult(int imported, int failed, List<ImportError> errors) {}

    private final EmployeeService employees;
    private final SalaryService salaries;

    public CsvService(EmployeeService employees, SalaryService salaries) {
        this.employees = employees;
        this.salaries = salaries;
    }

    static final String[] HEADERS = {"name", "department", "role", "country", "city",
            "joining_date", "status", "base_pay", "currency", "pay_frequency", "effective_from"};

    public ImportResult importEmployees(InputStream in) throws IOException {
        List<ImportError> errors = new ArrayList<>();
        int imported = 0;
        int failed = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return new ImportResult(0, 0, List.of(new ImportError(0, "empty file")));
            }
            String line;
            int row = 1; // 1-based data rows (header is row 0)
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    importRow(parseCsvLine(line));
                    imported++;
                } catch (RowException e) {
                    failed++;
                    errors.add(new ImportError(row, e.getMessage()));
                }
            }
        }
        return new ImportResult(imported, failed, errors);
    }

    private void importRow(String[] f) {
        if (f.length < 11) {
            throw new RowException("expected 11 columns, got " + f.length);
        }
        String name = f[0].trim(), department = f[1].trim(), role = f[2].trim();
        String country = f[3].trim(), city = f[4].trim();
        LocalDate joining;
        try {
            joining = LocalDate.parse(f[5].trim());
        } catch (DateTimeParseException e) {
            throw new RowException("joining_date '" + f[5] + "' is not YYYY-MM-DD");
        }
        String status = f[6].trim();
        List<String> errs = SalaryValidator.validateEmployee(
                name, department, role, country, joining, status);
        if (!errs.isEmpty()) {
            throw new RowException(String.join("; ", errs));
        }

        // Salary columns are optional per row, but if any is present all must be valid.
        // Validate the salary BEFORE creating the employee: a rejected row must
        // leave nothing behind (no half-created employee without a salary).
        boolean hasSalary = !f[7].isBlank() || !f[8].isBlank()
                || !f[9].isBlank() || !f[10].isBlank();
        BigDecimal basePay = null;
        String currency = null;
        SalaryRecord.PayFrequency frequency = null;
        LocalDate effective = null;
        if (hasSalary) {
            try {
                basePay = new BigDecimal(f[7].trim());
            } catch (NumberFormatException e) {
                throw new RowException("base_pay '" + f[7] + "' is not a number");
            }
            currency = f[8].trim();
            try {
                frequency = SalaryRecord.PayFrequency.valueOf(f[9].trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RowException("pay_frequency must be MONTHLY or ANNUAL");
            }
            try {
                effective = LocalDate.parse(f[10].trim());
            } catch (DateTimeParseException e) {
                throw new RowException("effective_from '" + f[10] + "' is not YYYY-MM-DD");
            }
            List<String> salaryErrs = SalaryValidator.validateRevision(
                    basePay, currency, frequency, effective, List.of());
            if (!salaryErrs.isEmpty()) {
                throw new RowException("salary rejected: " + String.join("; ", salaryErrs));
            }
        }

        Employee emp = employees.create(name, department, role, country,
                city.isEmpty() ? null : city, joining, status);
        if (hasSalary) {
            try {
                salaries.addRevision(emp.id(), basePay, currency, frequency, effective);
            } catch (Exception e) {
                // Should not happen after pre-validation; roll the row back
                // rather than leaving an employee with no salary.
                employees.delete(emp.id());
                throw new RowException("salary rejected: " + e.getMessage());
            }
        }
    }

    /** Minimal RFC-4180 line parser: handles quoted fields and "" escapes. */
    static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(String[]::new);
    }

    public static class RowException extends RuntimeException {
        RowException(String message) {
            super(message);
        }
    }

    /** Export one insight view as CSV text (header + rows). */
    public String exportInsights(String view, InsightsService insights) {
        StringBuilder out = new StringBuilder();
        switch (view) {
            case "by-department", "by-country", "by-role" -> {
                out.append("group,currency,headcount,average,median,min,max\n");
                List<InsightsService.GroupStats> rows = switch (view) {
                    case "by-department" -> insights.byDepartment();
                    case "by-country" -> insights.byCountry();
                    default -> insights.byRole();
                };
                for (InsightsService.GroupStats r : rows) {
                    out.append(csvCell(r.group())).append(",").append(r.currency()).append(",")
                            .append(r.headcount()).append(",").append(r.average()).append(",")
                            .append(r.median()).append(",").append(r.min()).append(",")
                            .append(r.max()).append("\n");
                }
            }
            case "distribution" -> {
                out.append("currency,band,from,to,headcount\n");
                for (InsightsService.Distribution d : insights.distribution()) {
                    for (InsightsService.Band b : d.bands()) {
                        out.append(d.currency()).append(",").append(csvCell(b.label())).append(",")
                                .append(b.from() == null ? "" : b.from()).append(",")
                                .append(b.to() == null ? "" : b.to()).append(",")
                                .append(b.headcount()).append("\n");
                    }
                }
            }
            default -> throw new IllegalArgumentException(
                    "unknown view '" + view + "' (by-department, by-country, by-role, distribution)");
        }
        return out.toString();
    }

    private String csvCell(String v) {
        if (v == null) {
            return "";
        }
        return (v.contains(",") || v.contains("\""))
                ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
