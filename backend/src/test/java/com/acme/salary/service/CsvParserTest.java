package com.acme.salary.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The hand-rolled RFC-4180 line parser used by CSV import. */
class CsvParserTest {

    @Test
    void plainLine() {
        assertArrayEquals(new String[]{"a", "b", "c"},
                CsvService.parseCsvLine("a,b,c"));
    }

    @Test
    void quotedFieldWithComma() {
        assertArrayEquals(new String[]{"Doe, Jane", "Engineering", "x"},
                CsvService.parseCsvLine("\"Doe, Jane\",Engineering,x"));
    }

    @Test
    void escapedQuotesInsideQuotedField() {
        assertArrayEquals(new String[]{"say \"hi\"", "b"},
                CsvService.parseCsvLine("\"say \"\"hi\"\"\",b"));
    }

    @Test
    void emptyTrailingFieldsKept() {
        String[] f = CsvService.parseCsvLine("a,b,,");
        assertEquals(4, f.length);
        assertEquals("", f[2]);
        assertEquals("", f[3]);
    }
}
