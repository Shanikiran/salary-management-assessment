package com.acme.salary.web;

import com.acme.salary.service.InsightsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Salary insights API. Every aggregate is grouped by currency and the
 * currency is always returned alongside the numbers — amounts are never
 * converted, because the brief names no FX source.
 */
@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insights;
    private final com.acme.salary.service.CsvService csv;

    public InsightsController(InsightsService insights,
            com.acme.salary.service.CsvService csv) {
        this.insights = insights;
        this.csv = csv;
    }

    @GetMapping("/by-department")
    public List<InsightsService.GroupStats> byDepartment() {
        return insights.byDepartment();
    }

    @GetMapping("/by-country")
    public List<InsightsService.GroupStats> byCountry() {
        return insights.byCountry();
    }

    @GetMapping("/by-role")
    public List<InsightsService.GroupStats> byRole() {
        return insights.byRole();
    }

    /** Same role across countries (optionally filtered to a country list). */
    @GetMapping("/compare-role")
    public List<InsightsService.GroupStats> compareRole(
            @RequestParam String role,
            @RequestParam(required = false) List<String> countries) {
        return insights.compareRole(role, countries);
    }

    /** Pay distribution per currency (percentile bands) + headcount per band. */
    @GetMapping("/distribution")
    public List<InsightsService.Distribution> distribution() {
        return insights.distribution();
    }

    @GetMapping("/headcount-by-band")
    public Map<String, List<InsightsService.Band>> headcountByBand() {
        return insights.headcountByBand();
    }

    /** CSV export of an insight view: by-department | by-country | by-role | distribution. */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@RequestParam(defaultValue = "by-department") String view) {
        String body = csv.exportInsights(view, insights);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"insights-" + view + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}
