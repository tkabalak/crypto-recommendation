package com.example.crypto.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cache configuration.
 *
 * <p>Uses Caffeine as an in-memory cache to reduce DB load for repeated queries.
 */
@Configuration
public class CacheConfig {

    public static final String CACHE_RECOMMENDATIONS = "recommendations";
    public static final String CACHE_STATS = "stats";
    public static final String CACHE_SUPPORTED = "supportedCryptos";
    public static final String CACHE_BEST_DAY = "bestForDay";

    /**
     * Creates the {@link CacheManager} used by the application.
     */
    @Bean
    public CacheManager cacheManager() {
        var manager = new CaffeineCacheManager(CACHE_RECOMMENDATIONS, CACHE_STATS, CACHE_SUPPORTED, CACHE_BEST_DAY);

        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(Duration.ofMinutes(5))
        );

        return manager;
    }
}
