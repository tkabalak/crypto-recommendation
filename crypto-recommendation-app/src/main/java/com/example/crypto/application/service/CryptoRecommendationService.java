package com.example.crypto.application.service;

import com.example.crypto.adapters.out.persistence.repository.PricePointRepository;
import com.example.crypto.adapters.out.persistence.repository.projection.SymbolMinMaxProjection;
import com.example.crypto.config.CacheConfig;
import com.example.crypto.domain.exception.NoDataForDayException;
import com.example.crypto.domain.exception.NoDataForRangeException;
import com.example.crypto.domain.exception.UnsupportedCryptoException;
import com.example.crypto.domain.model.CryptoStats;
import com.example.crypto.domain.model.PricePoint;
import com.example.crypto.domain.model.TimeRange;
import com.example.crypto.domain.service.NormalizedRangeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Application service exposing crypto statistics and recommendations.
 *
 * <p>All computations are based on price points already imported into the database at startup.
 */
@Service
@Transactional(readOnly = true)
public class CryptoRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(CryptoRecommendationService.class);


    private final PricePointRepository repo;
    private final NormalizedRangeCalculator rangeCalculator = new NormalizedRangeCalculator();

    public CryptoRecommendationService(PricePointRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns distinct supported crypto symbols currently stored in the database.
     *
     * <p>Result is cached to avoid repeated DB queries.
     */

    @Cacheable(cacheNames = CacheConfig.CACHE_SUPPORTED)
    public List<String> supportedSymbols() {
        var symbols = repo.findDistinctSymbols();
        log.info("Supported symbols loaded: count={}", symbols.size());
        return symbols;
    }

    /**
     * Resolves an effective time range from optional {@code from}/{@code to} dates.
     *
     * <ul>
     *   <li>If only one boundary is provided, it is treated as a single-day range.</li>
     *   <li>If both are provided, the range is validated.</li>
     *   <li>If neither is provided, the full dataset range from DB is used.</li>
     * </ul>
     */
    public TimeRange resolveRange(LocalDate from, LocalDate to) {
        log.info("Resolving time range: from={} to={}", from, to);

        // - if only one boundary is provided, treat it as a single-day range
        // - if both are provided, validate via TimeRange.of(from, to)
        if (from != null && to == null) {
            return TimeRange.of(from, from);
        }

        if (from == null && to != null) {
            return TimeRange.of(to, to);
        }

        if (from != null) { // and to != null
            return TimeRange.of(from, to);
        }

        // By default whole available dataset range in DB
        Instant min = repo.findMinTimestamp()
                .orElseThrow(() -> new NoDataForRangeException("ALL", Instant.EPOCH, Instant.EPOCH));

        Instant max = repo.findMaxTimestamp()
                .orElseThrow(() -> new NoDataForRangeException("ALL", Instant.EPOCH, Instant.EPOCH));

        // make toExclusive safely beyond max
        return new TimeRange(min, max.plusMillis(1));
    }

    /**
     * Returns a descending sorted list of cryptos by normalized range in the given time range.
     *
     * <p>Normalized range = (max - min) / min.
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_RECOMMENDATIONS,
            key = "#p0.fromInclusive().toString() + '|' + #p0.toExclusive().toString()")
    public List<RecommendationRow> recommendations(TimeRange range) {
        log.info("Calculating recommendations for range: {} -> {}", range.fromInclusive(), range.toExclusive());

        List<SymbolMinMaxProjection> rows = repo.findMinMaxBySymbolInRange(range.fromInclusive(), range.toExclusive());
        if (rows.isEmpty()) {
            throw new NoDataForRangeException("ALL", range.fromInclusive(), range.toExclusive());
        }

        return rows.stream()
                .map(r -> {
                    BigDecimal normalized = rangeCalculator.calculate(r.getMinPrice(), r.getMaxPrice());

                    return new RecommendationRow(r.getSymbol(), r.getMinPrice(), r.getMaxPrice(), normalized);
                })
                .sorted(Comparator.comparing(RecommendationRow::normalizedRange).reversed())
                .toList();
    }

    /**
     * Returns oldest/newest/min/max and normalized range for a requested crypto in the given time range.
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_STATS,
            key = "#p0 + '|' + #p1.fromInclusive().toString() + '|' + #p1.toExclusive().toString()")
    public CryptoStats stats(String symbol, TimeRange range) {
        log.info("Calculating stats for symbol={} range={} -> {}", symbol, range.fromInclusive(), range.toExclusive());

        String sym = normalize(symbol);
        ensureSupported(sym);

        var min = repo.findMinPriceForSymbolInRange(sym, range.fromInclusive(), range.toExclusive())
                .orElseThrow(() -> new NoDataForRangeException(sym, range.fromInclusive(), range.toExclusive()));
        var max = repo.findMaxPriceForSymbolInRange(sym, range.fromInclusive(), range.toExclusive())
                .orElseThrow(() -> new NoDataForRangeException(sym, range.fromInclusive(), range.toExclusive()));

        var oldest = repo.findFirstBySymbolAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                        sym, range.fromInclusive(), range.toExclusive())
                .orElseThrow(() -> new NoDataForRangeException(sym, range.fromInclusive(), range.toExclusive()));

        var newest = repo.findFirstBySymbolAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(
                        sym, range.fromInclusive(), range.toExclusive())
                .orElseThrow(() -> new NoDataForRangeException(sym, range.fromInclusive(), range.toExclusive()));

        var normalized = rangeCalculator.calculate(min, max);

        return new CryptoStats(sym, new PricePoint(oldest.getTimestamp(), oldest.getPriceUsd()),
                new PricePoint(newest.getTimestamp(), newest.getPriceUsd()), min, max, normalized
        );
    }


    /**
     * Returns the crypto with the highest normalized range for a specific day.
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_BEST_DAY, key = "#p0.toString()")
    public BestForDay bestForDay(LocalDate day) {
        log.info("Finding best crypto for day={}", day);

        var range = TimeRange.singleDay(day);
        List<SymbolMinMaxProjection> rows = repo.findMinMaxBySymbolInRange(range.fromInclusive(), range.toExclusive());

        if (rows.isEmpty()) {
            throw new NoDataForRangeException("ALL", range.fromInclusive(), range.toExclusive());
        }

        return rows.stream()
                .map(r -> {
                    BigDecimal normalized = rangeCalculator.calculate(r.getMinPrice(), r.getMaxPrice());

                    return new BestForDay(day, r.getSymbol(), r.getMinPrice(), r.getMaxPrice(), normalized);
                })
                .max(Comparator.comparing(BestForDay::normalizedRange))
                .orElseThrow(() -> new NoDataForDayException(day, "ALL", range.fromInclusive(), range.toExclusive()));
    }

    private void ensureSupported(String symbol) {
        if (symbol == null || symbol.isBlank() || !supportedSymbols().contains(symbol)) {
            throw new UnsupportedCryptoException(symbol == null ? "" : symbol);
        }
    }

    private String normalize(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase(Locale.ROOT);
    }

    public record RecommendationRow(String symbol, BigDecimal min, BigDecimal max, BigDecimal normalizedRange) {
    }

    public record BestForDay(LocalDate day, String symbol, BigDecimal min, BigDecimal max, BigDecimal normalizedRange) {
    }
}
