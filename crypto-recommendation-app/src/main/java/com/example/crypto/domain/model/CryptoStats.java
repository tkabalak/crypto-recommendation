package com.example.crypto.domain.model;

import java.math.BigDecimal;

/**
 * Aggregated statistics for a crypto in a specific time range.
 */
public record CryptoStats(
        String symbol,
        PricePoint oldest,
        PricePoint newest,
        BigDecimal min,
        BigDecimal max,
        BigDecimal normalizedRange
) {
}
