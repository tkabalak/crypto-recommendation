package com.example.crypto.domain.exception;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Thrown when there are no price points for any crypto for a requested day.
 * Mapped to HTTP 404 (application/problem+json).
 */
public class NoDataForDayException extends NoDataForPeriodException {

    private final LocalDate day;

    public NoDataForDayException(LocalDate day, String symbol, Instant fromInclusive, Instant toExclusive) {
        super(symbol, fromInclusive, toExclusive);
        this.day = day;
    }

    public LocalDate getDay() {
        return day;
    }
}
