package com.example.crypto.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import com.example.crypto.adapters.out.persistence.repository.PricePointRepository;
import com.example.crypto.adapters.out.persistence.repository.projection.SymbolMinMaxProjection;
import com.example.crypto.domain.exception.NoDataForDayException;
import com.example.crypto.domain.exception.NoDataForPeriodException;
import com.example.crypto.domain.exception.NoDataForRangeException;
import com.example.crypto.domain.exception.UnsupportedCryptoException;
import com.example.crypto.domain.model.TimeRange;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CryptoRecommendationService}.
 */
class CryptoRecommendationServiceTest {

  @Test
  void resolveRange_fromOnly_shouldCreateSingleDayRange() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    CryptoRecommendationService service = new CryptoRecommendationService(repo);

    LocalDate from = LocalDate.parse("2026-01-01");
    TimeRange range = service.resolveRange(from, null);

    assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
  }

  @Test
  void resolveRange_toOnly_shouldCreateSingleDayRange() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    CryptoRecommendationService service = new CryptoRecommendationService(repo);

    LocalDate to = LocalDate.parse("2026-01-05");
    TimeRange range = service.resolveRange(null, to);

    assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"));
    assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-01-06T00:00:00Z"));
  }

  @Test
  void resolveRange_none_shouldUseDbMinMax() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    Mockito.when(repo.findMinTimestamp()).thenReturn(Optional.of(Instant.parse("2026-01-01T00:00:00Z")));
    Mockito.when(repo.findMaxTimestamp()).thenReturn(Optional.of(Instant.parse("2026-01-31T23:59:59Z")));

    CryptoRecommendationService service = new CryptoRecommendationService(repo);
    TimeRange range = service.resolveRange(null, null);

    assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-01-31T23:59:59.001Z"));
  }

  @Test
  void recommendations_shouldReturnSortedByNormalizedRangeDesc() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    CryptoRecommendationService service = new CryptoRecommendationService(repo);

    TimeRange range = TimeRange.of(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-02"));

    SymbolMinMaxProjection btc = projection("BTC", new BigDecimal("10"), new BigDecimal("20")); // (20-10)/10=1.0
    SymbolMinMaxProjection eth = projection("ETH", new BigDecimal("10"), new BigDecimal("15")); // 0.5
    Mockito.when(repo.findMinMaxBySymbolInRange(range.fromInclusive(), range.toExclusive()))
        .thenReturn(List.of(eth, btc));

    List<CryptoRecommendationService.RecommendationRow> rows = service.recommendations(range);

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).symbol()).isEqualTo("BTC");
    assertThat(rows.get(1).symbol()).isEqualTo("ETH");
    assertThat(rows.get(0).normalizedRange()).isEqualByComparingTo("1.0");
  }

  @Test
  void recommendations_empty_shouldThrowNoDataForRange() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    CryptoRecommendationService service = new CryptoRecommendationService(repo);

    TimeRange range = TimeRange.singleDay(LocalDate.parse("2026-01-01"));
    Mockito.when(repo.findMinMaxBySymbolInRange(range.fromInclusive(), range.toExclusive()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.recommendations(range))
        .isInstanceOf(NoDataForPeriodException.class)
            .hasMessageContaining("No data for symbol 'ALL' in range [2026-01-01T00:00:00Z, 2026-01-02T00:00:00Z)");
  }

  @Test
  void stats_unsupportedSymbol_shouldThrow() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    Mockito.when(repo.findDistinctSymbols()).thenReturn(List.of("BTC", "ETH"));

    CryptoRecommendationService service = new CryptoRecommendationService(repo);
    TimeRange range = TimeRange.singleDay(LocalDate.parse("2026-01-01"));

    assertThatThrownBy(() -> service.stats("DOGE", range))
        .isInstanceOf(UnsupportedCryptoException.class);
  }

  @Test
  void bestForDay_shouldReturnMaxNormalizedRange() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    CryptoRecommendationService service = new CryptoRecommendationService(repo);

    LocalDate day = LocalDate.parse("2026-01-01");
    TimeRange range = TimeRange.singleDay(day);

    SymbolMinMaxProjection btc = projection("BTC", new BigDecimal("10"), new BigDecimal("20")); // 1.0
    SymbolMinMaxProjection eth = projection("ETH", new BigDecimal("10"), new BigDecimal("12")); // 0.2

    Mockito.when(repo.findMinMaxBySymbolInRange(range.fromInclusive(), range.toExclusive()))
        .thenReturn(List.of(eth, btc));

    CryptoRecommendationService.BestForDay best = service.bestForDay(day);
    assertThat(best.symbol()).isEqualTo("BTC");
    assertThat(best.normalizedRange()).isEqualByComparingTo("1.0");
  }

  @Test
  void bestForDay_noRows_shouldThrow() {
    PricePointRepository repo = Mockito.mock(PricePointRepository.class);
    CryptoRecommendationService service = new CryptoRecommendationService(repo);

    Mockito.when(repo.findMinMaxBySymbolInRange(Mockito.any(), Mockito.any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.bestForDay(LocalDate.parse("2026-01-01")))
        .isInstanceOf(NoDataForRangeException.class);
  }

  private static SymbolMinMaxProjection projection(String symbol, BigDecimal min, BigDecimal max) {
    SymbolMinMaxProjection p = Mockito.mock(SymbolMinMaxProjection.class);
    Mockito.when(p.getSymbol()).thenReturn(symbol);
    Mockito.when(p.getMinPrice()).thenReturn(min);
    Mockito.when(p.getMaxPrice()).thenReturn(max);
    return p;
  }
}
