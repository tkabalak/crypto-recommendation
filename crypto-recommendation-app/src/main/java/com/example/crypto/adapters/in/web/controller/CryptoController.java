package com.example.crypto.adapters.in.web.controller;

import com.example.crypto.adapters.in.web.dto.BestCryptoResponse;
import com.example.crypto.adapters.in.web.dto.CryptoStatsResponse;
import com.example.crypto.adapters.in.web.dto.PricePointDto;
import com.example.crypto.adapters.in.web.dto.RecommendationResponse;
import com.example.crypto.application.service.CryptoRecommendationService;
import com.example.crypto.domain.model.TimeRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller exposing crypto recommendation endpoints.
 */
@Validated
@RestController
@RequestMapping("/api/v1/cryptos")
public class CryptoController {

    private final CryptoRecommendationService service;

    public CryptoController(CryptoRecommendationService service) {
        this.service = service;
    }

    /**
     * Returns crypto symbols currently supported by the service (present in the database).
     *
     * <p>Useful for clients to avoid querying unsupported symbols.
     */
    @Operation(summary = "Returns supported crypto symbols")
    @ApiResponse(responseCode = "200", description = "Supported symbols")
    @GetMapping("/supported")
    public List<String> supported() {
        return service.supportedSymbols();
    }


    /**
     * Endpoint: recommendations.
     */
    @Operation(
            summary = "Returns cryptos sorted descending by normalized range",
            description = "normalizedRange = (max - min) / min"
    )
    @ApiResponse(responseCode = "200", description = "Sorted list")
    @GetMapping("/recommendations")
    public List<RecommendationResponse> recommendations(
            @RequestParam(required = false)
            @Parameter(example = "2026-01-01", description = "Start date (inclusive). If only from or to is provided, it is treated as a single-day range.")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @Parameter(example = "2026-01-31", description = "End date (inclusive).")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        TimeRange range = service.resolveRange(from, to);
        return service.recommendations(range).stream()
                .map(r -> new RecommendationResponse(r.symbol(), r.normalizedRange(), r.min(), r.max()))
                .toList();
    }

    /**
     * Endpoint: stats.
     */
    @Operation(summary = "Returns oldest/newest/min/max for requested crypto")
    @ApiResponse(responseCode = "200", description = "Stats for crypto")
    @ApiResponse(responseCode = "404", description = "Unsupported crypto or no data",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = com.example.crypto.adapters.in.web.dto.ProblemDetailDto.class)))
    @GetMapping("/{symbol}/stats")
    public CryptoStatsResponse stats(
            @PathVariable
            @Parameter(example = "BTC", description = "Crypto symbol (2-10 alphanumeric chars)")
            @Pattern(regexp = "^[A-Za-z0-9]{2,10}$", message = "symbol must be 2-10 alphanumeric chars")
            String symbol,
            @RequestParam(required = false)
            @Parameter(example = "2026-01-01", description = "Start date (inclusive). If only from or to is provided, it is treated as a single-day range.")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @Parameter(example = "2026-01-31", description = "End date (inclusive).")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        TimeRange range = service.resolveRange(from, to);
        var stats = service.stats(symbol, range);

        return new CryptoStatsResponse(
                stats.symbol(),
                new PricePointDto(stats.oldest().timestamp(), stats.oldest().priceUsd()),
                new PricePointDto(stats.newest().timestamp(), stats.newest().priceUsd()),
                stats.min(),
                stats.max(),
                stats.normalizedRange()
        );
    }

    @Operation(summary = "Returns crypto with highest normalized range for a specific day")
    @ApiResponse(responseCode = "200", description = "Best crypto for day")
    @ApiResponse(responseCode = "404", description = "No data for day",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = com.example.crypto.adapters.in.web.dto.ProblemDetailDto.class)))
    @GetMapping("/best")
    public BestCryptoResponse bestForDay(
            @RequestParam @NotNull
            @Parameter(example = "2026-01-01", description = "Requested day (UTC)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
    ) {
        var best = service.bestForDay(day);
        return new BestCryptoResponse(best.day(), best.symbol(), best.min(), best.max(), best.normalizedRange());
    }
}
