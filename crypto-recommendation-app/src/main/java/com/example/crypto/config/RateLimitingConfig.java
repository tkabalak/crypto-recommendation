package com.example.crypto.config;

import com.example.crypto.adapters.in.web.ratelimit.IpRateLimitingFilter;
import com.example.crypto.config.properties.RateLimitingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Registers IP-based rate limiting filter.
 */
@Configuration
@EnableConfigurationProperties(RateLimitingProperties.class)
public class RateLimitingConfig {

    /**
     * Rate limiting filter enabled by {@code app.rate-limit.enabled=true} (default).
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IpRateLimitingFilter ipRateLimitingFilter(RateLimitingProperties props, MeterRegistry registry,
                                                     HandlerExceptionResolver handlerExceptionResolver,
                                                     ObjectMapper objectMapper) {
        return new IpRateLimitingFilter(props, registry, handlerExceptionResolver, objectMapper);
    }
}
