-- ACME Salary Management — schema (SQLite)
-- Money is stored as NUMERIC and handled as BigDecimal in Java so decimal
-- pay amounts round-trip exactly (SQLite NUMERIC affinity preserves the
-- literal as entered; REAL/double would introduce binary float error).

CREATE TABLE IF NOT EXISTS employees (
  id           TEXT PRIMARY KEY,          -- EMP-00001
  name         TEXT NOT NULL,
  department   TEXT NOT NULL,
  role         TEXT NOT NULL,
  country      TEXT NOT NULL,
  city         TEXT,
  joining_date TEXT NOT NULL,            -- ISO date YYYY-MM-DD
  status       TEXT NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | ON_LEAVE | EXITED
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_emp_name    ON employees(name);
CREATE INDEX IF NOT EXISTS idx_emp_dept    ON employees(department);
CREATE INDEX IF NOT EXISTS idx_emp_country ON employees(country);
CREATE INDEX IF NOT EXISTS idx_emp_status  ON employees(status);

-- Salary history: every change is a new row, never an UPDATE of an old row.
-- A record's pay period runs from its effective_from until the next
-- record's effective_from (open-ended for the latest). Overlap is prevented
-- in the service layer: a new effective_from must be strictly after the
-- latest existing one, and the (employee_id, effective_from) pair is unique.
CREATE TABLE IF NOT EXISTS salary_records (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  employee_id    TEXT NOT NULL REFERENCES employees(id),
  base_pay       NUMERIC NOT NULL,
  currency       TEXT NOT NULL,           -- ISO 4217, e.g. INR, USD
  pay_frequency  TEXT NOT NULL,           -- MONTHLY | ANNUAL
  effective_from TEXT NOT NULL,           -- ISO date YYYY-MM-DD
  created_at     TEXT NOT NULL,
  UNIQUE(employee_id, effective_from)
);
CREATE INDEX IF NOT EXISTS idx_sal_emp ON salary_records(employee_id, effective_from);
