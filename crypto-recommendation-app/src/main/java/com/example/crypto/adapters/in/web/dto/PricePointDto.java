package com.example.crypto.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API DTO representing a price point.
 */
public record PricePointDto(@Schema(example = "2026-01-01T00:00:00Z") Instant timestamp,
                            @Schema(example = "46813.21") BigDecimal price) {
}
