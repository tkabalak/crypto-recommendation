package com.example.crypto.adapters.out.persistence.repository;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PricePointRepositoryTest {

    @Autowired
    private PricePointRepository repo;

    @Test
    void shouldComputeMinMaxBySymbolInRange() {
        repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("10")));
        repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T01:00:00Z"), new BigDecimal("15")));
        repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("20")));

        var from = Instant.parse("2026-01-01T00:00:00Z");
        var to = Instant.parse("2026-01-02T00:00:00Z");

        var rows = repo.findMinMaxBySymbolInRange(from, to);
        assertThat(rows).hasSize(2);

        var btc = rows.stream().filter(r -> r.getSymbol().equals("BTC")).findFirst().orElseThrow();
        assertThat(btc.getMinPrice()).isEqualByComparingTo("10");
        assertThat(btc.getMaxPrice()).isEqualByComparingTo("15");

        var eth = rows.stream().filter(r -> r.getSymbol().equals("ETH")).findFirst().orElseThrow();
        assertThat(eth.getMinPrice()).isEqualByComparingTo("20");
        assertThat(eth.getMaxPrice()).isEqualByComparingTo("20");
    }
}
