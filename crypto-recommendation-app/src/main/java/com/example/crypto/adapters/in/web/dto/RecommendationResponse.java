package com.example.crypto.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * API response DTO for a recommendation entry.
 */
public record RecommendationResponse(
        @Schema(example = "BTC") String symbol,
        @Schema(example = "0.1234") BigDecimal normalizedRange,
        @Schema(example = "45000.00") BigDecimal min,
        @Schema(example = "51000.00") BigDecimal max
) {
}
