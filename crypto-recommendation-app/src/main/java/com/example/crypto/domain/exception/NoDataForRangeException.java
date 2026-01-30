package com.example.crypto.domain.exception;

import java.time.Instant;

/**
 * Thrown when there are no price points for a given crypto (or all cryptos) in a requested time range.
 * Mapped to HTTP 404 (application/problem+json).
 */
public class NoDataForRangeException extends NoDataForPeriodException {

    public NoDataForRangeException(String symbol, Instant fromInclusive, Instant toExclusive) {
        super(symbol, fromInclusive, toExclusive);
    }
}
