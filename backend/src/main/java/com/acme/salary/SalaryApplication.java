package com.acme.salary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ACME Salary Management — Spring Boot entry point.
 * Serves the REST API on :8080 and, when the Angular production build is
 * copied into src/main/resources/static, the UI from the same jar.
 */
@SpringBootApplication
public class SalaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalaryApplication.class, args);
    }
}
