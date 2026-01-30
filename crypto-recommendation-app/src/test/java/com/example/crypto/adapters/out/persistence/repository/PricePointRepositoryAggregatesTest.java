package com.example.crypto.adapters.out.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository integration tests validating SQL/JPQL aggregates used by recommendation endpoints.
 */
@DataJpaTest
class PricePointRepositoryAggregatesTest {

  @Autowired
  PricePointRepository repo;

  @Test
  void shouldReturnOldestAndNewestWithinRange() {
    repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("10")));
    repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T12:00:00Z"), new BigDecimal("11")));
    repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-02T00:00:00Z"), new BigDecimal("12")));

    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z"); // exclusive

    var oldest = repo.findFirstBySymbolAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc("BTC", from, to).orElseThrow();
    var newest = repo.findFirstBySymbolAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampDesc("BTC", from, to).orElseThrow();

    assertThat(oldest.getTimestamp()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(newest.getTimestamp()).isEqualTo(Instant.parse("2026-01-01T12:00:00Z"));
  }

  @Test
  void shouldReturnMinAndMaxForSymbolWithinRange() {
    repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("20")));
    repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T01:00:00Z"), new BigDecimal("19")));
    repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T02:00:00Z"), new BigDecimal("25")));
    repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-02T00:00:00Z"), new BigDecimal("1"))); // out of range

    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");

    var min = repo.findMinPriceForSymbolInRange("ETH", from, to).orElseThrow();
    var max = repo.findMaxPriceForSymbolInRange("ETH", from, to).orElseThrow();

    assertThat(min).isEqualByComparingTo("19");
    assertThat(max).isEqualByComparingTo("25");
  }

  @Test
  void shouldAggregateMinMaxBySymbolWithinRange() {
    repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("10")));
    repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T01:00:00Z"), new BigDecimal("15")));
    repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("100")));
    repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T01:00:00Z"), new BigDecimal("90")));

    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");

    var rows = repo.findMinMaxBySymbolInRange(from, to);
    assertThat(rows).hasSize(2);

    var btc = rows.stream().filter(r -> r.getSymbol().equals("BTC")).findFirst().orElseThrow();
    assertThat(btc.getMinPrice()).isEqualByComparingTo("10");
    assertThat(btc.getMaxPrice()).isEqualByComparingTo("15");

    var eth = rows.stream().filter(r -> r.getSymbol().equals("ETH")).findFirst().orElseThrow();
    assertThat(eth.getMinPrice()).isEqualByComparingTo("90");
    assertThat(eth.getMaxPrice()).isEqualByComparingTo("100");
  }
}
