package com.example.crypto.domain.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NormalizedRangeCalculatorTest {

    private final NormalizedRangeCalculator calc = new NormalizedRangeCalculator();

    @Test
    void shouldCalculateNormalizedRange() {
        BigDecimal min = new BigDecimal("10");
        BigDecimal max = new BigDecimal("15");
        BigDecimal result = calc.calculate(min, max);

        // (15-10)/10 = 0.5
        assertEquals(0, result.compareTo(new BigDecimal("0.5")));
    }

    @Test
    void shouldRejectZeroOrNegativeMin() {
        assertThrows(IllegalArgumentException.class, () -> calc.calculate(BigDecimal.ZERO, new BigDecimal("1")));
        assertThrows(IllegalArgumentException.class, () -> calc.calculate(new BigDecimal("-1"), new BigDecimal("1")));
    }
}
