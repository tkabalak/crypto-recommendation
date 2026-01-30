package com.example.crypto.adapters.in.web.error;

import com.example.crypto.domain.exception.InvalidDateRangeException;
import com.example.crypto.domain.exception.NoDataForPeriodException;
import com.example.crypto.domain.exception.UnsupportedCryptoException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that directly exercise {@link GlobalExceptionHandler} mappings.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerCoverageIT.ThrowingController.class)
class GlobalExceptionHandlerCoverageIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldMapUnsupportedCryptoTo404Problem() throws Exception {
        mockMvc.perform(get("/test/errors/unsupported"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Unsupported crypto symbol")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.symbol", is("DOGE")));
    }

    @Test
    void shouldMapNoDataTo404Problem() throws Exception {
        mockMvc.perform(get("/test/errors/nodata"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("No data for requested period")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void shouldMapInvalidDateRangeTo400Problem() throws Exception {
        mockMvc.perform(get("/test/errors/invalid-range"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Invalid date range")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void shouldMapConstraintViolationTo400Problem() throws Exception {
        mockMvc.perform(get("/test/errors/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Bad request")))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void shouldMapUnhandledExceptionTo500Problem() throws Exception {
        mockMvc.perform(get("/test/errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("RuntimeException")));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/errors/unsupported")
        void unsupported() {
            throw new UnsupportedCryptoException("DOGE");
        }

        @GetMapping("/test/errors/nodata")
        void nodata() {
            throw new NoDataForPeriodException("BTC",
                    Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2026-01-02T00:00:00Z"));
        }

        @GetMapping("/test/errors/invalid-range")
        void invalidRange() {
            throw new InvalidDateRangeException(LocalDate.parse("2026-01-10"), LocalDate.parse("2026-01-01"));
        }

        @GetMapping("/test/errors/constraint")
        void constraint() {
            throw new ConstraintViolationException("invalid", Set.of());
        }

        @GetMapping("/test/errors/boom")
        void boom() {
            throw new RuntimeException("boom");
        }
    }
}
