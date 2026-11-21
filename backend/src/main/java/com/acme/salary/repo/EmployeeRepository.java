package com.acme.salary.repo;

import com.acme.salary.model.Employee;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Employee persistence via Spring JdbcTemplate (explicit SQL, no ORM). */
@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbc;

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static final RowMapper<Employee> MAPPER = (rs, rowNum) -> new Employee(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("department"),
            rs.getString("role"),
            rs.getString("country"),
            rs.getString("city"),
            SqlDates.parse(rs.getString("joining_date")),
            Employee.EmploymentStatus.valueOf(rs.getString("status")),
            SqlDates.parse(rs.getString("created_at")),
            SqlDates.parse(rs.getString("updated_at"))
    );

    /** Paginated, filtered search. Never returns the whole table at once. */
    public List<Employee> search(String name, String department, String country,
            String status, String sort, String order, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM employees WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            sql.append(" AND name LIKE ?");
            args.add("%" + name.trim() + "%");
        }
        if (department != null && !department.isBlank()) {
            sql.append(" AND department = ?");
            args.add(department.trim());
        }
        if (country != null && !country.isBlank()) {
            sql.append(" AND country = ?");
            args.add(country.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status.trim().toUpperCase());
        }
        sql.append(" ORDER BY ").append(sortColumn(sort)).append("asc".equalsIgnoreCase(order) ? " ASC" : " DESC");
        sql.append(" LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public long count(String name, String department, String country, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM employees WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            sql.append(" AND name LIKE ?");
            args.add("%" + name.trim() + "%");
        }
        if (department != null && !department.isBlank()) {
            sql.append(" AND department = ?");
            args.add(department.trim());
        }
        if (country != null && !country.isBlank()) {
            sql.append(" AND country = ?");
            args.add(country.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status.trim().toUpperCase());
        }
        Long n = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    public Employee findById(String id) {
        List<Employee> rows = jdbc.query(
                "SELECT * FROM employees WHERE id = ?", MAPPER, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insert(Employee e) {
        jdbc.update("""
                INSERT INTO employees (id, name, department, role, country, city,
                    joining_date, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                e.id(), e.name(), e.department(), e.role(), e.country(), e.city(),
                e.joiningDate().toString(), e.status().name(),
                e.createdAt().toString(), e.updatedAt().toString());
    }

    public int update(Employee e) {
        return jdbc.update("""
                UPDATE employees SET name = ?, department = ?, role = ?, country = ?,
                    city = ?, joining_date = ?, status = ?, updated_at = ?
                WHERE id = ?""",
                e.name(), e.department(), e.role(), e.country(), e.city(),
                e.joiningDate().toString(), e.status().name(),
                e.updatedAt().toString(), e.id());
    }

    public int delete(String id) {
        return jdbc.update("DELETE FROM employees WHERE id = ?", id);
    }

    /** Next EMP- id: one past the current numeric maximum (EMP-00001 …). */
    public String nextId() {
        Integer max = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTR(id, 5) AS INTEGER)) FROM employees", Integer.class);
        return "EMP-" + String.format("%05d", (max == null ? 0 : max) + 1);
    }

    public long totalCount() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM employees", Long.class);
        return n == null ? 0 : n;
    }

    private String sortColumn(String sort) {
        return switch (sort == null ? "name" : sort) {
            case "joining_date" -> "joining_date";
            case "department" -> "department";
            case "country" -> "country";
            default -> "name";
        };
    }
}
