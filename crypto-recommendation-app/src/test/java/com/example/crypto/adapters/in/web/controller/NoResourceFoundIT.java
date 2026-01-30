package com.example.crypto.adapters.in.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test ensuring unknown paths return RFC7807 problem+json instead of 500.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class NoResourceFoundIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void unknownPathShouldReturn404ProblemJson() throws Exception {
        mockMvc.perform(get("/this-path-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Not Found")));
    }
}
