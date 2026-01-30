package com.example.crypto.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response DTO for the best crypto (highest normalized range) for a day.
 */
public record BestCryptoResponse(
        @Schema(example = "2026-01-01") LocalDate day,
        @Schema(example = "BTC") String symbol,
        @Schema(example = "0.1333") BigDecimal min,
        @Schema(example = "0.1333") BigDecimal max,
        @Schema(example = "0.1333") BigDecimal normalizedRange
) {
}
