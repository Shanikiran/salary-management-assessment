package com.acme.salary.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {

    private static List<BigDecimal> vals(int... xs) {
        return java.util.Arrays.stream(xs).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void medianOddCount() {
        assertEquals(BigDecimal.valueOf(2), Stats.median(vals(1, 3, 2)));
    }

    @Test
    void medianEvenCountAveragesMiddleTwo() {
        assertEquals(new BigDecimal("2.50"), Stats.median(vals(1, 2, 3, 4)));
    }

    @Test
    void medianEmptyIsNull() {
        assertNull(Stats.median(List.of()));
    }

    @Test
    void medianIsOrderIndependent() {
        assertEquals(BigDecimal.valueOf(5), Stats.median(vals(9, 1, 5, 7, 3)));
    }

    @Test
    void averageRoundsHalfUp() {
        assertEquals(new BigDecimal("2.00"), Stats.average(vals(1, 2, 3)));
        assertEquals(new BigDecimal("1.67"), Stats.average(vals(1, 2, 2)));
    }

    @Test
    void percentileNearestRank() {
        List<BigDecimal> v = vals(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        assertEquals(BigDecimal.valueOf(30), Stats.percentile(v, 25));
        assertEquals(BigDecimal.valueOf(50), Stats.percentile(v, 50));
        assertEquals(BigDecimal.valueOf(90), Stats.percentile(v, 90));
        assertEquals(BigDecimal.valueOf(100), Stats.percentile(v, 100));
    }
}
