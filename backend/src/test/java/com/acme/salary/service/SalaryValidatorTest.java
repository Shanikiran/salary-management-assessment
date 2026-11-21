package com.acme.salary.service;

import com.acme.salary.model.SalaryRecord;
import com.acme.salary.validation.SalaryValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation rules, from the requirements doc: no negative amounts,
 * currency required and ISO, no overlapping effective periods.
 */
class SalaryValidatorTest {

    private static final LocalDate JAN = LocalDate.of(2025, 1, 1);
    private static final LocalDate JUL = LocalDate.of(2025, 7, 1);

    private List<SalaryRecord> history(SalaryRecord... records) {
        return List.of(records);
    }

    private SalaryRecord record(LocalDate from) {
        return new SalaryRecord(1L, "EMP-00001", BigDecimal.valueOf(100000),
                "INR", SalaryRecord.PayFrequency.ANNUAL, from, JAN);
    }

    @Test
    void firstRecordWithValidValuesPasses() {
        assertTrue(SalaryValidator.validateRevision(
                BigDecimal.valueOf(1200000), "INR",
                SalaryRecord.PayFrequency.ANNUAL, JAN, List.of()).isEmpty());
    }

    @Test
    void negativeAmountRejected() {
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.valueOf(-5000), "INR",
                SalaryRecord.PayFrequency.ANNUAL, JAN, List.of());
        assertTrue(errors.stream().anyMatch(e -> e.contains("greater than zero")));
    }

    @Test
    void zeroAmountRejected() {
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.ZERO, "INR",
                SalaryRecord.PayFrequency.ANNUAL, JAN, List.of());
        assertFalse(errors.isEmpty());
    }

    @Test
    void missingCurrencyRejected() {
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.valueOf(1000), null,
                SalaryRecord.PayFrequency.ANNUAL, JAN, List.of());
        assertTrue(errors.stream().anyMatch(e -> e.contains("currency is required")));
    }

    @Test
    void invalidCurrencyCodeRejected() {
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.valueOf(1000), "ZZZ",
                SalaryRecord.PayFrequency.ANNUAL, JAN, List.of());
        assertTrue(errors.stream().anyMatch(e -> e.contains("ISO 4217")));
    }

    @Test
    void lowercaseCurrencyCodeAccepted() {
        // Codes are normalised to upper-case by the service; the validator
        // itself accepts any case of a real ISO code.
        assertTrue(SalaryValidator.validateRevision(
                BigDecimal.valueOf(1000), "inr",
                SalaryRecord.PayFrequency.ANNUAL, JAN, List.of()).isEmpty());
    }

    @Test
    void duplicateEffectiveDateRejected() {
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.valueOf(110000), "INR",
                SalaryRecord.PayFrequency.ANNUAL, JAN, history(record(JAN)));
        assertTrue(errors.stream().anyMatch(e -> e.contains("already exists")));
    }

    @Test
    void earlierEffectiveDateRejected() {
        // A date before the latest record would split an existing period.
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.valueOf(110000), "INR",
                SalaryRecord.PayFrequency.ANNUAL, JAN, history(record(JAN), record(JUL)));
        assertTrue(errors.stream().anyMatch(e -> e.contains("must be after the latest")));
    }

    @Test
    void laterEffectiveDateAccepted() {
        assertTrue(SalaryValidator.validateRevision(
                BigDecimal.valueOf(110000), "INR",
                SalaryRecord.PayFrequency.ANNUAL, LocalDate.of(2026, 1, 1),
                history(record(JAN), record(JUL))).isEmpty());
    }

    @Test
    void missingFrequencyRejected() {
        List<String> errors = SalaryValidator.validateRevision(
                BigDecimal.valueOf(1000), "INR", null, JAN, List.of());
        assertTrue(errors.stream().anyMatch(e -> e.contains("payFrequency")));
    }

    @Test
    void employeeValidationRejectsBlankNameAndFutureJoiningDate() {
        List<String> errors = SalaryValidator.validateEmployee(
                " ", "Engineering", "Software Engineer", "India",
                LocalDate.now().plusDays(1), "ACTIVE");
        assertEquals(2, errors.size());
    }

    @Test
    void employeeValidationRejectsBadStatus() {
        List<String> errors = SalaryValidator.validateEmployee(
                "Asha", "Engineering", "Software Engineer", "India",
                LocalDate.of(2024, 1, 1), "FIRED");
        assertTrue(errors.stream().anyMatch(e -> e.contains("ACTIVE, ON_LEAVE, EXITED")));
    }
}
