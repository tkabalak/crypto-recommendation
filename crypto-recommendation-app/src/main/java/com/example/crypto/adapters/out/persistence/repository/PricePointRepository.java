package com.example.crypto.adapters.out.persistence.repository;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import com.example.crypto.adapters.out.persistence.repository.projection.SymbolMinMaxProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing crypto price points.
 *
 * <p>Contains aggregate queries used by recommendation/statistics endpoints.
 */
public interface PricePointRepository extends JpaRepository<PricePointEntity, Long> {

    /**
     * Returns distinct supported crypto symbols present in the database.
     */
    @Query("""
            select distinct p.symbol
            from PricePointEntity p
            order by p.symbol
            """)
    List<String> findDistinctSymbols();

    /**
     * @return the minimum timestamp across all stored price points.
     */
    @Query("select min(p.timestamp) from PricePointEntity p")
    Optional<Instant> findMinTimestamp();

    /**
     * @return the maximum timestamp across all stored price points.
     */
    @Query("select max(p.timestamp) from PricePointEntity p")
    Optional<Instant> findMaxTimestamp();

    /**
     * Aggregates min and max price per symbol in a time range.
     *
     * @param from inclusive lower bound
     * @param to   exclusive upper bound
     * @return list of (symbol, minPrice, maxPrice)
     */
    @Query("""
            select p.symbol as symbol,
                   min(p.priceUsd) as minPrice,
                   max(p.priceUsd) as maxPrice
            from PricePointEntity p
            where p.timestamp >= :from and p.timestamp < :to
            group by p.symbol
            """)
    List<SymbolMinMaxProjection> findMinMaxBySymbolInRange(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * @return min price for a given symbol within a time range.
     */
    @Query("""
            select min(p.priceUsd)
            from PricePointEntity p
            where p.symbol = :symbol and p.timestamp >= :from and p.timestamp < :to
            """)
    Optional<BigDecimal> findMinPriceForSymbolInRange(@Param("symbol") String symbol,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to);

    /**
     * @return max price for a given symbol within a time range.
     * @return oldest (earliest) price point for a symbol within a time range.
     */
    @Query("""
            select max(p.priceUsd)
            from PricePointEntity p
            where p.symbol = :symbol and p.timestamp >= :from and p.timestamp < :to
            """)
    Optional<BigDecimal> findMaxPriceForSymbolInRange(@Param("symbol") String symbol,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to);

    /**
     * @return oldest (earliest) price point for a symbol within a time range.
     */
    Optional<PricePointEntity> findFirstBySymbolAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
            String symbol, Instant fromInclusive, Instant toExclusive);

    /**
     * @return newest (latest) price point for a symbol within a time range.
     */
    Optional<PricePointEntity> findFirstBySymbolAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc(
            String symbol, Instant fromInclusive, Instant toExclusive);
}
