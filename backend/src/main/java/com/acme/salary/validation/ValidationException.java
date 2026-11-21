package com.acme.salary.validation;

import java.util.ArrayList;
import java.util.List;

/** Thrown when a request fails business validation; mapped to HTTP 400. */
public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String error) {
        this(List.of(error));
    }

    public List<String> errors() {
        return errors;
    }

    /** Small helper so validators can collect errors fluently. */
    public static class Collector {
        private final List<String> errors = new ArrayList<>();

        public void add(boolean invalid, String message) {
            if (invalid) {
                errors.add(message);
            }
        }

        public void check() {
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
        }
    }
}
