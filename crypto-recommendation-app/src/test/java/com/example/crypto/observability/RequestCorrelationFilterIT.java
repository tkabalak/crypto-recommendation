package com.example.crypto.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for request correlation (X-Request-Id propagation).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RequestCorrelationFilterIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldReturnXRequestIdWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/supported"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.HEADER_REQUEST_ID, notNullValue()));
    }

    @Test
    void shouldEchoXRequestIdWhenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/supported").header(RequestCorrelationFilter.HEADER_REQUEST_ID, "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.HEADER_REQUEST_ID, "abc-123"));
    }
}
