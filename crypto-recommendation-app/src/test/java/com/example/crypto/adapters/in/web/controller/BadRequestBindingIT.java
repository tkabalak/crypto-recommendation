package com.example.crypto.adapters.in.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers request binding / type mismatch errors mapped by {@code GlobalExceptionHandler}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BadRequestBindingIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void invalidDayFormatShouldReturn400ProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/cryptos/best").param("day", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title", is("Bad request")))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
