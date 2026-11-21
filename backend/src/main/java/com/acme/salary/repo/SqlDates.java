package com.acme.salary.repo;

import java.time.LocalDate;

/**
 * SQLite stores dates as TEXT, so a date column can legally hold either a
 * plain date ({@code 2024-03-01}) or an ISO datetime
 * ({@code 2024-03-01T09:00:00Z}) — the seed script used to write the latter.
 * Parse the date part either way instead of failing on a valid row.
 */
final class SqlDates {

    private SqlDates() {
    }

    static LocalDate parse(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.length() > 10 ? LocalDate.parse(s.substring(0, 10)) : LocalDate.parse(s);
    }
}
