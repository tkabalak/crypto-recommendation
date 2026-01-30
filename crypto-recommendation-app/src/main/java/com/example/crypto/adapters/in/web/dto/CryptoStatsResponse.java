package com.example.crypto.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * API response DTO for the crypto statuses (symbol, oldest, newest, min and max) for a range.
 */
public record CryptoStatsResponse(
        @Schema(example = "BTC") String symbol,
        PricePointDto oldest,
        PricePointDto newest,
        @Schema(example = "46813.21") BigDecimal min,
        @Schema(example = "46813.21") BigDecimal max,
        @Schema(example = "46813.21") BigDecimal normalizedRange
) {
}
