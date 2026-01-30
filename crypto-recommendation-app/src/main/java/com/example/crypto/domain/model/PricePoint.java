package com.example.crypto.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable timestamped price point.
 *
 * @param timestamp timestamp (UTC)
 * @param priceUsd  price in USD
 */
public record PricePoint(Instant timestamp, BigDecimal priceUsd) {
}
