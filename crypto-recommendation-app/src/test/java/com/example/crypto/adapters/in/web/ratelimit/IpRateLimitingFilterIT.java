package com.example.crypto.adapters.in.web.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test verifying IP-based rate limiting.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.import.enabled=false",
        "app.rate-limit.enabled=true",
        "app.rate-limit.capacity=2",
        "app.rate-limit.refill-tokens=2",
        "app.rate-limit.refill-duration=PT1M",
        "app.rate-limit.max-buckets=100",
        "app.rate-limit.bucket-expire-after-access=PT5M"
})
class IpRateLimitingFilterIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldReturn429AfterLimitExceeded() throws Exception {
        // First two requests from same IP should pass (may still return 404 if endpoint doesn't exist)
        mockMvc.perform(get("/api/v1/cryptos/supported").header("X-Forwarded-For", "1.2.3.4"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cryptos/supported").header("X-Forwarded-For", "1.2.3.4"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cryptos/supported").header("X-Forwarded-For", "1.2.3.4"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title", is("Rate limit exceeded")))
                .andExpect(jsonPath("$.status", is(429)));
    }
}
