package com.acme.salary.service;

import com.acme.salary.repo.SalaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Insights math over a mocked current-pay snapshot (no database needed). */
@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    SalaryRepository salaries;

    private SalaryRepository.CurrentPay pay(String dept, String role, String country,
            String currency, int amount) {
        return new SalaryRepository.CurrentPay("EMP-1", dept, role, country,
                BigDecimal.valueOf(amount), currency, "ANNUAL", LocalDate.of(2025, 1, 1));
    }

    @Test
    void groupsAreSplitByCurrencyNeverMixed() {
        when(salaries.allCurrentPay(any())).thenReturn(List.of(
                pay("Engineering", "Software Engineer", "India", "INR", 1_200_000),
                pay("Engineering", "Software Engineer", "India", "INR", 2_400_000),
                pay("Engineering", "Software Engineer", "USA", "USD", 120_000)));
        InsightsService svc = new InsightsService(salaries);

        List<InsightsService.GroupStats> rows = svc.byDepartment();
        assertEquals(2, rows.size()); // Engineering/INR and Engineering/USD
        InsightsService.GroupStats inr = rows.stream()
                .filter(r -> r.currency().equals("INR")).findFirst().orElseThrow();
        assertEquals(2, inr.headcount());
        assertEquals(new BigDecimal("1800000.00"), inr.average());
        assertEquals(new BigDecimal("1800000.00"), inr.median());
        assertEquals(BigDecimal.valueOf(1_200_000), inr.min());
        assertEquals(BigDecimal.valueOf(2_400_000), inr.max());
    }

    @Test
    void compareRoleShowsEachCountryWithItsOwnCurrency() {
        when(salaries.allCurrentPay(any())).thenReturn(List.of(
                pay("Engineering", "Software Engineer", "India", "INR", 1_200_000),
                pay("Engineering", "Software Engineer", "USA", "USD", 120_000),
                pay("Engineering", "QA Engineer", "India", "INR", 900_000)));
        InsightsService svc = new InsightsService(salaries);

        List<InsightsService.GroupStats> rows = svc.compareRole("Software Engineer", null);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(r -> r.group().equals("India") && r.currency().equals("INR")));
        assertTrue(rows.stream().anyMatch(r -> r.group().equals("USA") && r.currency().equals("USD")));
    }

    @Test
    void distributionHeadcountsSumToTotalPerCurrency() {
        when(salaries.allCurrentPay(any())).thenReturn(List.of(
                pay("Engineering", "Software Engineer", "India", "INR", 600_000),
                pay("Engineering", "Software Engineer", "India", "INR", 900_000),
                pay("Engineering", "Software Engineer", "India", "INR", 1_200_000),
                pay("Engineering", "Software Engineer", "India", "INR", 1_800_000),
                pay("Engineering", "Software Engineer", "India", "INR", 3_000_000),
                pay("Engineering", "Software Engineer", "USA", "USD", 100_000),
                pay("Engineering", "Software Engineer", "USA", "USD", 150_000)));
        InsightsService svc = new InsightsService(salaries);

        List<InsightsService.Distribution> dist = svc.distribution();
        assertEquals(2, dist.size());
        for (InsightsService.Distribution d : dist) {
            int total = d.bands().stream().mapToInt(InsightsService.Band::headcount).sum();
            int expected = d.currency().equals("INR") ? 5 : 2;
            assertEquals(expected, total, "bands for " + d.currency() + " must sum to headcount");
        }
    }

    @Test
    void emptyDataProducesNoGroups() {
        when(salaries.allCurrentPay(any())).thenReturn(List.of());
        InsightsService svc = new InsightsService(salaries);
        assertTrue(svc.byDepartment().isEmpty());
        assertTrue(svc.distribution().isEmpty());
    }
}
