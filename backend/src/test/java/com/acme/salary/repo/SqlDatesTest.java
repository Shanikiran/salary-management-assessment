package com.acme.salary.repo;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SqlDatesTest {

    @Test
    void plainDate() {
        assertEquals(LocalDate.of(2024, 3, 1), SqlDates.parse("2024-03-01"));
    }

    @Test
    void isoDatetimeTakesDatePart() {
        assertEquals(LocalDate.of(2024, 3, 1), SqlDates.parse("2024-03-01T09:00:00Z"));
    }

    @Test
    void nullStaysNull() {
        assertNull(SqlDates.parse(null));
    }
}
