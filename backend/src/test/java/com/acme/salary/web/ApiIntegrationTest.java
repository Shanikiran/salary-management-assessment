package com.acme.salary.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end API test against a real (temporary) SQLite database:
 * employee CRUD, append-only salary history, validation rejections,
 * pagination, CSV import/export, and insights.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("test.db"));
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    /** Fresh tables for every test — the class shares one temp DB file. */
    @BeforeEach
    void cleanDb() {
        jdbc.execute("DELETE FROM salary_records");
        jdbc.execute("DELETE FROM employees");
    }

    private String createEmployeeJson() {
        return """
                {"name":"Asha Nair","department":"Engineering","role":"Software Engineer",
                 "country":"India","city":"Pune","joiningDate":"2024-03-01","status":"ACTIVE",
                 "basePay":1200000,"currency":"INR","payFrequency":"ANNUAL",
                 "effectiveFrom":"2024-03-01"}""";
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void fullEmployeeAndSalaryFlow() throws Exception {
        // Create with an initial salary.
        String created = mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("EMP-00001"))
                .andReturn().getResponse().getContentAsString();
        assert created.contains("EMP-00001");

        // Current salary is the initial record.
        mvc.perform(get("/api/employees/EMP-00001/salaries/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePay").value(1200000))
                .andExpect(jsonPath("$.currency").value("INR"));

        // Add a revision with a later effective date.
        mvc.perform(post("/api/employees/EMP-00001/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"basePay":1350000,"currency":"INR",
                                 "payFrequency":"ANNUAL","effectiveFrom":"2025-03-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.basePay").value(1350000));

        // History keeps BOTH records; newest first.
        mvc.perform(get("/api/employees/EMP-00001/salaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].basePay").value(1350000))
                .andExpect(jsonPath("$[1].basePay").value(1200000));

        // Current salary is now the revision.
        mvc.perform(get("/api/employees/EMP-00001/salaries/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePay").value(1350000));

        // Detail endpoint bundles employee + current + history.
        mvc.perform(get("/api/employees/EMP-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.name").value("Asha Nair"))
                .andExpect(jsonPath("$.currentSalary.basePay").value(1350000))
                .andExpect(jsonPath("$.salaryHistory.length()").value(2));
    }

    @Test
    void validationRejectsBadSalaryRevision() throws Exception {
        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson()))
                .andExpect(status().isCreated());

        // Negative amount.
        mvc.perform(post("/api/employees/EMP-00001/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"basePay":-100,"currency":"INR",
                                 "payFrequency":"ANNUAL","effectiveFrom":"2025-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("greater than zero")));

        // Duplicate effective date.
        mvc.perform(post("/api/employees/EMP-00001/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"basePay":1300000,"currency":"INR",
                                 "payFrequency":"ANNUAL","effectiveFrom":"2024-03-01"}"""))
                .andExpect(status().isBadRequest());

        // Invalid currency code.
        mvc.perform(post("/api/employees/EMP-00001/salaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"basePay":1300000,"currency":"ZZZ",
                                 "payFrequency":"ANNUAL","effectiveFrom":"2025-06-01"}"""))
                .andExpect(status().isBadRequest());

        // Unknown employee.
        mvc.perform(get("/api/employees/EMP-99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void paginationNeverReturnsEverything() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mvc.perform(post("/api/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Emp %d","department":"Sales","role":"Sales Executive",
                                     "country":"India","city":"Mumbai","joiningDate":"2023-01-01",
                                     "status":"ACTIVE"}""".formatted(i)))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/employees").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        mvc.perform(get("/api/employees").param("department", "Sales"))
                .andExpect(jsonPath("$.totalElements").value(5));
        mvc.perform(get("/api/employees").param("name", "Emp 3"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void csvImportReportsRowLevelErrors() throws Exception {
        String csv = """
                name,department,role,country,city,joining_date,status,base_pay,currency,pay_frequency,effective_from
                Ravi Kumar,Engineering,Software Engineer,India,Pune,2024-01-15,ACTIVE,900000,INR,ANNUAL,2024-01-15
                Bad Pay,Engineering,Software Engineer,India,Pune,2024-01-15,ACTIVE,-50000,INR,ANNUAL,2024-01-15
                No Currency,Engineering,Software Engineer,India,Pune,2024-01-15,ACTIVE,900000,,ANNUAL,2024-01-15
                """;
        mvc.perform(multipart("/api/employees/import")
                        .file(new MockMultipartFile("file", "import.csv",
                                "text/csv", csv.getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.failed").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2));
        // A rejected row must leave nothing behind: only the 1 valid
        // employee exists, no half-created rows without a salary.
        mvc.perform(get("/api/employees").param("size", "100"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void insightsAndCsvExportWork() throws Exception {
        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createEmployeeJson()))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John Smith","department":"Engineering",
                                 "role":"Software Engineer","country":"USA","city":"Austin",
                                 "joiningDate":"2023-06-01","status":"ACTIVE",
                                 "basePay":120000,"currency":"USD","payFrequency":"ANNUAL",
                                 "effectiveFrom":"2023-06-01"}"""))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/insights/by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Engineering/INR + Engineering/USD
                .andExpect(jsonPath("$[?(@.currency=='INR')].headcount").value(1));

        mvc.perform(get("/api/insights/compare-role").param("role", "Software Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/insights/distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/insights/export").param("view", "by-department"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("insights-by-department.csv")))
                .andExpect(content().string(containsString("group,currency,headcount,average,median,min,max")));
    }
}
