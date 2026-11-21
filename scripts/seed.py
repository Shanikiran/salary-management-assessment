#!/usr/bin/env python3
"""Seed script for the ACME salary-management assessment database.

Generates exactly 10,000 deterministic employees (random.seed(42)) with
salary history, matching backend/src/main/resources/schema.sql exactly.

Design choices
--------------
* Pay frequency: ALL salaries are ANNUAL (pay_frequency = 'ANNUAL') and
  expressed in the employee's local currency. This keeps analytics
  comparable within a currency group (e.g. average pay per department
  in INR) instead of mixing monthly and annual figures that would need
  normalisation before any comparison. Currency conversion is out of
  scope for the seed data.
* Salaries are role-scaled within each country's annual range: junior
  roles sit near the low end, managers near the high end.
* Salary history: every employee has 1 initial record (effective_from
  = joining date). ~25% get a second record (revision 6-18 months
  later, +5-15%); ~5% get a third record. Effective dates are strictly
  increasing per employee and never in the future.
* Deterministic: random.seed(42) — every run produces identical data.

Usage
-----
    python3 scripts/seed.py [--db PATH] [--reset]

Default DB: backend/data/salary.db (relative to the repo root).
If the DB already holds 10,000 employees and --reset is not given, the
script prints a message and exits 0 without duplicating anything.
"""

import argparse
import random
import sqlite3
import sys
import time
from datetime import date, timedelta
from pathlib import Path

SEED = 42
EMPLOYEE_COUNT = 10_000
TODAY = date(2026, 11, 20)

REPO_ROOT = Path(__file__).resolve().parent.parent
SCHEMA_PATH = REPO_ROOT / "backend" / "src" / "main" / "resources" / "schema.sql"
DEFAULT_DB = REPO_ROOT / "backend" / "data" / "salary.db"

# ---------------------------------------------------------------------------
# Reference data
# ---------------------------------------------------------------------------
# country -> (currency, cities, salary range (low, high), round-to,
#             first names, last names)
COUNTRIES = {
    "India": (
        "INR",
        ["Pune", "Mumbai", "Bengaluru", "Hyderabad", "Chennai"],
        (600_000, 3_500_000), 1_000,
        ["Aarav", "Vihaan", "Arjun", "Rohan", "Kabir", "Ananya", "Priya",
         "Sneha", "Kavya", "Ishita", "Vikram", "Rahul", "Neha", "Aditya",
         "Meera", "Kiran", "Sanjay", "Divya", "Nikhil", "Pooja"],
        ["Sharma", "Patel", "Nair", "Iyer", "Reddy", "Gupta", "Mehta",
         "Kulkarni", "Deshmukh", "Joshi", "Singh", "Agarwal", "Chopra",
         "Menon", "Pillai", "Bhatt", "Nimbalkar", "Khan", "Das", "Verma"],
    ),
    "United States": (
        "USD",
        ["New York", "San Francisco", "Austin", "Seattle"],
        (70_000, 220_000), 500,
        ["James", "Mary", "John", "Emma", "Michael", "Olivia", "David",
         "Sophia", "Daniel", "Ava", "Matthew", "Mia", "Andrew", "Ella",
         "Joshua", "Grace", "Ryan", "Chloe", "Nathan", "Lily"],
        ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia",
         "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez",
         "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore",
         "Martin", "Lee"],
    ),
    "United Kingdom": (
        "GBP",
        ["London", "Manchester", "Edinburgh"],
        (45_000, 130_000), 500,
        ["Oliver", "Amelia", "George", "Isla", "Harry", "Ava", "Jack",
         "Emily", "Charlie", "Mia", "Thomas", "Grace", "William", "Freya",
         "Henry", "Evie", "Alfie", "Lily", "Archie", "Sophie"],
        ["Smith", "Jones", "Taylor", "Brown", "Williams", "Wilson",
         "Johnson", "Davies", "Robinson", "Wright", "Thompson", "White",
         "Walker", "Harris", "Martin", "Clarke", "Lewis", "Young", "King",
         "Scott"],
    ),
    "Germany": (
        "EUR",
        ["Berlin", "Munich", "Hamburg"],
        (55_000, 130_000), 500,
        ["Lukas", "Jonas", "Leon", "Finn", "Paul", "Hannah", "Mia",
         "Emma", "Sofia", "Lea", "Maximilian", "Felix", "Tim", "Laura",
         "Julia", "Sarah", "Anna", "Lisa", "David", "Jan"],
        ["Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer",
         "Wagner", "Becker", "Hoffmann", "Schulz", "Koch", "Bauer",
         "Richter", "Klein", "Wolf", "Neumann", "Schwarz", "Zimmermann",
         "Krüger", "Hofmann"],
    ),
    "France": (
        "EUR",
        ["Paris", "Lyon"],
        (48_000, 110_000), 500,
        ["Lucas", "Hugo", "Nathan", "Louis", "Gabriel", "Emma", "Jade",
         "Louise", "Chloé", "Alice", "Raphaël", "Théo", "Léa", "Manon",
         "Camille", "Sarah", "Antoine", "Maxime", "Clara", "Inès"],
        ["Martin", "Bernard", "Dubois", "Moreau", "Laurent", "Simon",
         "Michel", "Lefebvre", "Leroy", "Roux", "David", "Bertrand",
         "Morel", "Fournier", "Girard", "Bonnet", "Dupont", "Lambert",
         "Fontaine", "Rousseau"],
    ),
    "Japan": (
        "JPY",
        ["Tokyo", "Osaka"],
        (5_000_000, 13_000_000), 10_000,
        ["Haruto", "Yuto", "Sota", "Yuki", "Ren", "Hina", "Yui", "Aoi",
         "Sakura", "Mei", "Kaito", "Daiki", "Riku", "Sora", "Akari",
         "Rin", "Takeshi", "Kenji", "Naomi", "Yumi"],
        ["Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito",
         "Yamamoto", "Nakamura", "Kobayashi", "Kato", "Yoshida",
         "Yamada", "Sasaki", "Yamaguchi", "Saito", "Matsumoto",
         "Inoue", "Kimura", "Hayashi", "Shimizu"],
    ),
    "Australia": (
        "AUD",
        ["Sydney", "Melbourne"],
        (80_000, 190_000), 500,
        ["Jack", "Oliver", "William", "Charlotte", "Mia", "Amelia",
         "Harper", "Ella", "Henry", "Leo", "Grace", "Isla", "Ruby",
         "Evie", "Noah", "Liam", "Mason", "Sophie", "Chloe", "Ethan"],
        ["Smith", "Jones", "Williams", "Brown", "Wilson", "Taylor",
         "Johnson", "White", "Martin", "Anderson", "Thomas", "Walker",
         "Harris", "Lee", "Clark", "Lewis", "Young", "King", "Wright",
         "Scott"],
    ),
    "Canada": (
        "CAD",
        ["Toronto", "Vancouver"],
        (75_000, 180_000), 500,
        ["Liam", "Noah", "Emma", "Olivia", "Lucas", "Mia", "Ethan",
         "Ava", "Mason", "Sophia", "Logan", "Isabella", "James",
         "Charlotte", "Benjamin", "Amelia", "Jacob", "Harper", "Elijah",
         "Ella"],
        ["Smith", "Brown", "Tremblay", "Martin", "Roy", "Gagnon",
         "Wilson", "Lee", "Johnson", "MacDonald", "Taylor", "White",
         "Thompson", "Harris", "Clark", "Lewis", "Walker", "Young",
         "King", "Wright"],
    ),
    "Singapore": (
        "SGD",
        ["Singapore"],
        (70_000, 180_000), 500,
        ["Wei", "Jun", "Hao", "Li", "Xiu", "Mei", "Jia", "Xin", "Arun",
         "Priya", "Raj", "Siti", "Ahmad", "Nur", "Grace", "Daniel",
         "Sarah", "Kevin", "Michelle", "Jason"],
        ["Tan", "Lim", "Lee", "Ng", "Ong", "Wong", "Chan", "Teo", "Goh",
         "Chua", "Kaur", "Singh", "Menon", "Chen", "Ho", "Kumar",
         "Ali", "Ahmad", "Sim", "Yeo"],
    ),
    "Brazil": (
        "BRL",
        ["São Paulo", "Rio de Janeiro"],
        (90_000, 320_000), 1_000,
        ["Miguel", "Arthur", "Helena", "Alice", "Theo", "Valentina",
         "Davi", "Laura", "Gabriel", "Sophia", "Lucas", "Isabella",
         "Rafael", "Manuela", "Enzo", "Júlia", "Pedro", "Lorena",
         "Guilherme", "Beatriz"],
        ["Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira",
         "Alves", "Pereira", "Lima", "Gomes", "Costa", "Ribeiro",
         "Martins", "Carvalho", "Almeida", "Lopes", "Soares", "Fernandes",
         "Vieira", "Barbosa"],
    ),
}

# Country weights (share of the 10,000 employees). India-heavy, matching
# ACME's largest office footprint; every country is represented.
COUNTRY_WEIGHTS = {
    "India": 30, "United States": 15, "United Kingdom": 10,
    "Germany": 8, "France": 6, "Japan": 7, "Australia": 5,
    "Canada": 6, "Singapore": 5, "Brazil": 8,
}

# department -> [(role, pay factor)] — factor scales the country's range.
DEPARTMENTS = {
    "Engineering": [
        ("Software Engineer", 1.00),
        ("Senior Software Engineer", 1.20),
        ("Engineering Manager", 1.40),
        ("QA Engineer", 0.90),
    ],
    "Product": [
        ("Product Manager", 1.25),
        ("Senior Product Manager", 1.40),
        ("Product Analyst", 0.90),
        ("Product Designer", 1.00),
    ],
    "Sales": [
        ("Sales Executive", 0.95),
        ("Senior Sales Executive", 1.15),
        ("Account Manager", 1.05),
        ("Sales Manager", 1.35),
    ],
    "Marketing": [
        ("Marketing Executive", 0.90),
        ("Content Strategist", 0.95),
        ("Marketing Manager", 1.30),
        ("SEO Specialist", 0.90),
    ],
    "Finance": [
        ("Financial Analyst", 1.00),
        ("Senior Financial Analyst", 1.20),
        ("Accountant", 0.95),
        ("Finance Manager", 1.35),
    ],
    "Human Resources": [
        ("HR Executive", 0.90),
        ("Talent Acquisition Specialist", 0.95),
        ("HR Business Partner", 1.15),
        ("HR Manager", 1.30),
    ],
    "Operations": [
        ("Operations Executive", 0.90),
        ("Operations Analyst", 1.00),
        ("Supply Chain Manager", 1.25),
        ("Operations Manager", 1.30),
    ],
    "Customer Success": [
        ("Customer Success Executive", 0.90),
        ("Support Engineer", 1.00),
        ("Customer Success Manager", 1.25),
        ("Technical Account Manager", 1.15),
    ],
}

JOIN_START = date(2018, 1, 1)
JOIN_END = TODAY  # joining dates 2018-2026, never in the future

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def random_date(start: date, end: date) -> date:
    """Uniform random date in [start, end]."""
    return start + timedelta(days=random.randint(0, (end - start).days))


def add_months(d: date, months: int) -> date:
    """Add months to a date, clamping the day to the target month's length."""
    month = d.month - 1 + months
    year = d.year + month // 12
    month = month % 12 + 1
    day = min(d.day, [31, 29 if year % 4 == 0 and (year % 100 != 0 or year % 400 == 0) else 28,
                      31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1])
    return date(year, month, day)


def round_to(value: float, step: int) -> int:
    return int(round(value / step) * step)


def salary_for(country: str, factor: float) -> int:
    """Annual salary in the country's currency, role-scaled and rounded."""
    _, _, (low, high), step, _, _ = COUNTRIES[country]
    base = random.uniform(low, high)
    amount = round_to(base * factor, step)
    return max(low, min(high, amount))


# ---------------------------------------------------------------------------
# Generation
# ---------------------------------------------------------------------------


def generate_employees():
    """Yield (employee_row, salary_rows) for all 10,000 employees."""
    countries = list(COUNTRY_WEIGHTS)
    weights = [COUNTRY_WEIGHTS[c] for c in countries]
    departments = list(DEPARTMENTS)
    # Timestamps are derived from the record's own date (09:00 UTC) so the
    # seeded data is fully deterministic — no wall-clock values anywhere.

    for i in range(1, EMPLOYEE_COUNT + 1):
        emp_id = f"EMP-{i:05d}"
        country = random.choices(countries, weights=weights, k=1)[0]
        currency, cities, _, _, firsts, lasts = COUNTRIES[country]
        department = random.choice(departments)
        role, factor = random.choice(DEPARTMENTS[department])
        name = f"{random.choice(firsts)} {random.choice(lasts)}"
        city = random.choice(cities)
        joining = random_date(JOIN_START, JOIN_END)
        status_roll = random.random()
        status = "ACTIVE" if status_roll < 0.90 else (
            "ON_LEAVE" if status_roll < 0.95 else "EXITED")

        emp_row = (emp_id, name, department, role, country, city,
                   joining.isoformat(), status,
                   joining.isoformat(),
                   joining.isoformat())

        # Salary history: 1 initial record, ~25% a 2nd, ~5% a 3rd.
        records = []
        amount = salary_for(country, factor)
        records.append((emp_id, amount, currency, "ANNUAL",
                        joining.isoformat(), joining.isoformat()))
        history_roll = random.random()
        n_records = 3 if history_roll < 0.05 else (2 if history_roll < 0.25 else 1)
        prev_date, prev_amount = joining, amount
        for _ in range(n_records - 1):
            eff = add_months(prev_date, random.randint(6, 18))
            if eff > TODAY:
                break  # revision would land in the future; stop history here
            low, high = COUNTRIES[country][2]
            prev_amount = max(low, min(high, round_to(
                prev_amount * random.uniform(1.05, 1.15),
                COUNTRIES[country][3])))  # clamp: revisions stay in range
            records.append((emp_id, prev_amount, currency, "ANNUAL",
                            eff.isoformat(), eff.isoformat()))
            prev_date = eff

        yield emp_row, records


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Seed the ACME salary-management DB with 10,000 employees.")
    parser.add_argument("--db", type=Path, default=DEFAULT_DB,
                        help="SQLite DB path (default: backend/data/salary.db)")
    parser.add_argument("--reset", action="store_true",
                        help="Drop existing tables and rebuild from scratch.")
    args = parser.parse_args()

    random.seed(SEED)
    start = time.monotonic()

    db_path = args.db
    db_path.parent.mkdir(parents=True, exist_ok=True)

    conn = sqlite3.connect(db_path)
    try:
        if args.reset:
            conn.executescript(
                "DROP TABLE IF EXISTS salary_records;"
                "DROP TABLE IF EXISTS employees;")
            conn.commit()

        # Always run the schema first so the DB is created correctly.
        schema = SCHEMA_PATH.read_text(encoding="utf-8")
        conn.executescript(schema)

        existing = conn.execute(
            "SELECT COUNT(*) FROM employees").fetchone()[0]
        if existing == EMPLOYEE_COUNT and not args.reset:
            print(f"Database already seeded: {existing} employees in {db_path}.")
            print("Nothing to do. Use --reset to drop and rebuild.")
            return 0
        if existing > 0 and not args.reset:
            print(f"ERROR: database already has {existing} employees "
                  f"(expected 0 or {EMPLOYEE_COUNT}). Refusing to add more "
                  f"to avoid duplicates. Use --reset to rebuild.",
                  file=sys.stderr)
            return 1

        employees, salaries = [], []
        for emp_row, sal_rows in generate_employees():
            employees.append(emp_row)
            salaries.extend(sal_rows)

        # One transaction, batched inserts.
        with conn:
            conn.executemany(
                "INSERT INTO employees (id, name, department, role, country,"
                " city, joining_date, status, created_at, updated_at)"
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                employees)
            conn.executemany(
                "INSERT INTO salary_records (employee_id, base_pay, currency,"
                " pay_frequency, effective_from, created_at)"
                " VALUES (?, ?, ?, ?, ?, ?)",
                salaries)
    finally:
        conn.close()

    elapsed = time.monotonic() - start

    # Summary (fresh connection, reading back what was actually stored).
    conn = sqlite3.connect(db_path)
    try:
        emp_count = conn.execute(
            "SELECT COUNT(*) FROM employees").fetchone()[0]
        sal_count = conn.execute(
            "SELECT COUNT(*) FROM salary_records").fetchone()[0]
        per_country = conn.execute(
            "SELECT country, COUNT(*) FROM employees"
            " GROUP BY country ORDER BY COUNT(*) DESC").fetchall()
    finally:
        conn.close()

    print(f"Seeded {db_path}")
    print(f"Employees inserted: {emp_count}")
    print(f"Salary records inserted: {sal_count}")
    print("Employees per country:")
    for country, count in per_country:
        print(f"  {country}: {count}")
    print(f"Elapsed: {elapsed:.2f}s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
