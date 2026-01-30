package com.example.crypto.adapters.in.web.controller;

import com.example.crypto.adapters.out.persistence.jpa.PricePointEntity;
import com.example.crypto.adapters.out.persistence.repository.PricePointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CryptoControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PricePointRepository repo;

    @BeforeEach
    void setup() {
        repo.deleteAll();
        repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("10")));
        repo.save(new PricePointEntity("BTC", Instant.parse("2026-01-01T23:00:00Z"), new BigDecimal("15")));
        repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("20")));
        repo.save(new PricePointEntity("ETH", Instant.parse("2026-01-01T23:00:00Z"), new BigDecimal("22")));
    }

    @Test
    void supportedShouldReturnDistinctSymbols() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/supported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("BTC", "ETH")));
    }


    @Test
    void recommendationsShouldReturnSortedByNormalizedRange() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/recommendations")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].symbol", anyOf(is("BTC"), is("ETH"))))
                .andExpect(jsonPath("$[0].normalizedRange", notNullValue()));
    }

    @Test
    void statsShouldReturnOldestNewestMinMax() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/BTC/stats")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("BTC")))
                .andExpect(jsonPath("$.min", closeTo(10.0, 0.000001)))
                .andExpect(jsonPath("$.max", closeTo(15.0, 0.000001)))
                .andExpect(jsonPath("$.oldest.timestamp", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.newest.timestamp", is("2026-01-01T23:00:00Z")));
    }

    @Test
    void statsForUnsupportedSymbolShouldReturnProblemJson404() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/DOGE/stats")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title", is("Unsupported crypto symbol")))
                .andExpect(jsonPath("$.symbol", is("DOGE")));
    }

    @Test
    void bestForDayShouldReturnCrypto() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/best")
                        .param("day", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day", is("2026-01-01")))
                .andExpect(jsonPath("$.symbol", anyOf(is("BTC"), is("ETH"))))
                .andExpect(jsonPath("$.normalizedRange", notNullValue()));
    }


    @Test
    void recommendationsWhenNoDataShouldReturn404ProblemJson() throws Exception {
        repo.deleteAll();

        mockMvc.perform(get("/api/v1/cryptos/recommendations")
                        .param("from", "2017-01-01")
                        .param("to", "2017-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title", is("No data for requested period")));
    }

    @Test
    void bestForDayWhenNoDataShouldReturn404ProblemJson() throws Exception {
        repo.deleteAll();

        mockMvc.perform(get("/api/v1/cryptos/best")
                        .param("day", "2026-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title", is("No data for requested period")));
    }

    @Test
    void invalidDateRangeShouldReturn400ProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/recommendations")
                        .param("from", "2026-01-02")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title", is("Invalid date range")));
    }

    @Test
    void invalidSymbolShouldReturn400ProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/BT!/stats")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title", is("Bad request")))
                .andExpect(jsonPath("$.violations", not(empty())));
    }

}
