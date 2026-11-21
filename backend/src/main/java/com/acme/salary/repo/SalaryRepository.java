package com.acme.salary.repo;

import com.acme.salary.model.SalaryRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Salary-history persistence. Rows are inserted, never updated. */
@Repository
public class SalaryRepository {

    private final JdbcTemplate jdbc;

    public SalaryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static final RowMapper<SalaryRecord> MAPPER = (rs, rowNum) -> new SalaryRecord(
            rs.getLong("id"),
            rs.getString("employee_id"),
            rs.getBigDecimal("base_pay"),
            rs.getString("currency"),
            SalaryRecord.PayFrequency.valueOf(rs.getString("pay_frequency")),
            SqlDates.parse(rs.getString("effective_from")),
            SqlDates.parse(rs.getString("created_at"))
    );

    /** Full history for one employee, newest effective date first. */
    public List<SalaryRecord> history(String employeeId) {
        return jdbc.query(
                "SELECT * FROM salary_records WHERE employee_id = ? ORDER BY effective_from DESC",
                MAPPER, employeeId);
    }

    /**
     * Current pay: the record with the latest effective_from on or before
     * today. A future-dated revision does not become "current" until its
     * effective date arrives.
     */
    public SalaryRecord current(String employeeId, LocalDate today) {
        List<SalaryRecord> rows = jdbc.query(
                """
                SELECT * FROM salary_records
                WHERE employee_id = ? AND effective_from <= ?
                ORDER BY effective_from DESC LIMIT 1
                """,
                MAPPER, employeeId, today.toString());
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insert(SalaryRecord r) {
        jdbc.update("""
                INSERT INTO salary_records (employee_id, base_pay, currency,
                    pay_frequency, effective_from, created_at)
                VALUES (?, ?, ?, ?, ?, ?)""",
                r.employeeId(), r.basePay(), r.currency(), r.payFrequency().name(),
                r.effectiveFrom().toString(), r.createdAt().toString());
    }

    public int deleteForEmployee(String employeeId) {
        return jdbc.update("DELETE FROM salary_records WHERE employee_id = ?", employeeId);
    }

    /** One row per employee: their current salary joined to employee facts. */
    public List<CurrentPay> allCurrentPay(LocalDate today) {
        return jdbc.query("""
                SELECT e.id AS employee_id, e.department, e.role, e.country,
                       s.base_pay, s.currency, s.pay_frequency, s.effective_from
                FROM employees e
                JOIN salary_records s ON s.employee_id = e.id
                WHERE s.effective_from <= ?
                  AND s.effective_from = (
                      SELECT MAX(s2.effective_from) FROM salary_records s2
                      WHERE s2.employee_id = e.id AND s2.effective_from <= ?)
                """,
                (rs, rowNum) -> new CurrentPay(
                        rs.getString("employee_id"),
                        rs.getString("department"),
                        rs.getString("role"),
                        rs.getString("country"),
                        rs.getBigDecimal("base_pay"),
                        rs.getString("currency"),
                        rs.getString("pay_frequency"),
                        SqlDates.parse(rs.getString("effective_from"))),
                today.toString(), today.toString());
    }

    public record CurrentPay(String employeeId, String department, String role,
            String country, BigDecimal basePay, String currency,
            String payFrequency, LocalDate effectiveFrom) {}
}
