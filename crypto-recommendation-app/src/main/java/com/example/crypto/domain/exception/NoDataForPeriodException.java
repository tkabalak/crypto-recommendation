package com.example.crypto.domain.exception;

import java.time.Instant;

/**
 * Thrown when there is no data for a requested symbol and time range.
 */
public class NoDataForPeriodException extends RuntimeException {
    private final String symbol;
    private final Instant fromInclusive;
    private final Instant toExclusive;

    public NoDataForPeriodException(String symbol, Instant fromInclusive, Instant toExclusive) {
        super("No data for symbol '%s' in range [%s, %s)".formatted(symbol, fromInclusive, toExclusive));

        this.symbol = symbol;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
    }

    /**
     * @return symbol related to the requested range
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @return inclusive lower bound
     */
    public Instant getFromInclusive() {
        return fromInclusive;
    }

    /**
     * @return exclusive upper bound
     */
    public Instant getToExclusive() {
        return toExclusive;
    }
}
