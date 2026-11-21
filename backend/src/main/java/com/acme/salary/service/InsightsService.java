package com.acme.salary.service;

import com.acme.salary.repo.SalaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Salary insights. All aggregates are computed over CURRENT pay only (latest
 * effective record on or before today). Amounts are never converted between
 * currencies — every aggregate is grouped by currency, and the currency is
 * always shown next to the number. That is a deliberate product decision:
 * the brief names no FX source, so converting with a guessed rate would
 * invent data.
 */
@Service
public class InsightsService {

    private final SalaryRepository salaries;

    public InsightsService(SalaryRepository salaries) {
        this.salaries = salaries;
    }

    public record GroupStats(String group, String currency, int headcount,
            BigDecimal average, BigDecimal median,
            BigDecimal min, BigDecimal max) {}

    public record Band(String label, BigDecimal from, BigDecimal to, int headcount) {}
    public record Distribution(String currency, List<Band> bands) {}

    private List<SalaryRepository.CurrentPay> currentPay() {
        return salaries.allCurrentPay(LocalDate.now());
    }

    /** Average/median/min/max pay per group, split by currency. */
    public List<GroupStats> byDimension(java.util.function.Function<SalaryRepository.CurrentPay, String> dim) {
        Map<String, Map<String, List<BigDecimal>>> grouped = new TreeMap<>();
        for (SalaryRepository.CurrentPay p : currentPay()) {
            grouped.computeIfAbsent(dim.apply(p), k -> new TreeMap<>())
                    .computeIfAbsent(p.currency(), k -> new ArrayList<>())
                    .add(p.basePay());
        }
        List<GroupStats> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<BigDecimal>>> g : grouped.entrySet()) {
            for (Map.Entry<String, List<BigDecimal>> c : g.getValue().entrySet()) {
                List<BigDecimal> vals = c.getValue();
                List<BigDecimal> sorted = vals.stream().sorted().toList();
                out.add(new GroupStats(g.getKey(), c.getKey(), vals.size(),
                        Stats.average(vals), Stats.median(vals),
                        sorted.get(0), sorted.get(sorted.size() - 1)));
            }
        }
        return out;
    }

    public List<GroupStats> byDepartment() {
        return byDimension(SalaryRepository.CurrentPay::department);
    }

    public List<GroupStats> byCountry() {
        return byDimension(SalaryRepository.CurrentPay::country);
    }

    public List<GroupStats> byRole() {
        return byDimension(SalaryRepository.CurrentPay::role);
    }

    /**
     * Same role across countries: per-country current-pay stats, currency
     * shown as stored. No conversion — the numbers are not directly
     * comparable across currencies and the UI says so.
     */
    public List<GroupStats> compareRole(String role, List<String> countries) {
        List<SalaryRepository.CurrentPay> rows = currentPay().stream()
                .filter(p -> p.role().equalsIgnoreCase(role))
                .filter(p -> countries == null || countries.isEmpty()
                        || countries.stream().anyMatch(c -> c.equalsIgnoreCase(p.country())))
                .toList();
        Map<String, Map<String, List<BigDecimal>>> grouped = new TreeMap<>();
        for (SalaryRepository.CurrentPay p : rows) {
            grouped.computeIfAbsent(p.country(), k -> new TreeMap<>())
                    .computeIfAbsent(p.currency(), k -> new ArrayList<>())
                    .add(p.basePay());
        }
        List<GroupStats> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<BigDecimal>>> g : grouped.entrySet()) {
            for (Map.Entry<String, List<BigDecimal>> c : g.getValue().entrySet()) {
                List<BigDecimal> vals = c.getValue();
                List<BigDecimal> sorted = vals.stream().sorted().toList();
                out.add(new GroupStats(g.getKey(), c.getKey(), vals.size(),
                        Stats.average(vals), Stats.median(vals),
                        sorted.get(0), sorted.get(sorted.size() - 1)));
            }
        }
        return out;
    }

    /**
     * Pay distribution per currency as percentile bands (P25 / P50 / P75 /
     * P90 cut-offs computed from the data itself), with headcount per band.
     * Percentile bands are used instead of fixed thresholds because a fixed
     * threshold (e.g. "under 50k") means something different in INR vs JPY.
     */
    public List<Distribution> distribution() {
        Map<String, List<BigDecimal>> byCurrency = currentPay().stream()
                .collect(Collectors.groupingBy(SalaryRepository.CurrentPay::currency,
                        TreeMap::new,
                        Collectors.mapping(SalaryRepository.CurrentPay::basePay,
                                Collectors.toList())));
        List<Distribution> out = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> e : byCurrency.entrySet()) {
            List<BigDecimal> vals = e.getValue();
            BigDecimal p25 = Stats.percentile(vals, 25);
            BigDecimal p50 = Stats.percentile(vals, 50);
            BigDecimal p75 = Stats.percentile(vals, 75);
            BigDecimal p90 = Stats.percentile(vals, 90);
            List<Band> bands = List.of(
                    band("Bottom 25%", null, p25, vals),
                    band("25–50%", p25, p50, vals),
                    band("50–75%", p50, p75, vals),
                    band("75–90%", p75, p90, vals),
                    band("Top 10%", p90, null, vals));
            out.add(new Distribution(e.getKey(), bands));
        }
        return out;
    }

    private Band band(String label, BigDecimal from, BigDecimal to, List<BigDecimal> vals) {
        int n = 0;
        for (BigDecimal v : vals) {
            // Bands are [from, to): a boundary value belongs to the higher band,
            // and the maximum value is always inside the open-ended top band.
            boolean aboveFrom = from == null || v.compareTo(from) >= 0;
            boolean belowTo = to == null || v.compareTo(to) < 0;
            if (aboveFrom && belowTo) {
                n++;
            }
        }
        return new Band(label, from, to, n);
    }

    /** Headcount per band = same distribution, flattened for the dashboard. */
    public Map<String, List<Band>> headcountByBand() {
        Map<String, List<Band>> out = new LinkedHashMap<>();
        for (Distribution d : distribution()) {
            out.put(d.currency(), d.bands());
        }
        return out;
    }
}
