package com.example.crypto.domain.exception;

import java.time.LocalDate;

/**
 * Thrown when a client provides an invalid date range (to < from).
 */
public class InvalidDateRangeException extends RuntimeException {
    private final LocalDate from;
    private final LocalDate to;

    public InvalidDateRangeException(LocalDate from, LocalDate to) {
        super("Invalid date range: from=%s, to=%s".formatted(from, to));
        this.from = from;
        this.to = to;
    }

    /**
     * @return from date
     */
    public LocalDate getFrom() {
        return from;
    }

    /**
     * @return to date
     */
    public LocalDate getTo() {
        return to;
    }
}
