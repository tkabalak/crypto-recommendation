package com.example.crypto.config;

import com.example.crypto.observability.RequestCorrelationFilter;
import com.example.crypto.observability.ServiceInstanceId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability-related beans (request correlation, MDC).
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Registers the request correlation filter (X-Request-Id + MDC).
     */
    @Bean
    public RequestCorrelationFilter requestCorrelationFilter(ServiceInstanceId serviceInstanceId) {
        return new RequestCorrelationFilter(serviceInstanceId);
    }
}
