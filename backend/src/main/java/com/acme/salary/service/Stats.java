package com.acme.salary.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small deterministic statistics helpers for the insights service.
 * All money math uses BigDecimal; medians/percentiles are computed on
 * sorted copies so callers' lists are never mutated.
 */
public final class Stats {

    private Stats() {}

    public static BigDecimal median(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2))
                .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
    }

    public static BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    /** Nearest-rank percentile (p in 0..100). */
    public static BigDecimal percentile(List<BigDecimal> values, double p) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        rank = Math.max(1, Math.min(rank, sorted.size()));
        return sorted.get(rank - 1);
    }
}
