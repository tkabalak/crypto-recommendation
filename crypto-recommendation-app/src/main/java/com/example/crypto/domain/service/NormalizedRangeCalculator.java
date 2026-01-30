package com.example.crypto.domain.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * Calculates the normalized range used for crypto recommendations.
 *
 * <p>Formula: (max - min) / min
 *
 * <p>Uses {@link java.math.MathContext#DECIMAL64} for stable precision.
 */
public class NormalizedRangeCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;

    /**
     * normalizedRange = (max - min) / min
     */
    public BigDecimal calculate(BigDecimal min, BigDecimal max) {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");

        if (min.signum() <= 0) {
            throw new IllegalArgumentException("min must be > 0");
        }

        return max.subtract(min, MC).divide(min, MC);
    }
}
