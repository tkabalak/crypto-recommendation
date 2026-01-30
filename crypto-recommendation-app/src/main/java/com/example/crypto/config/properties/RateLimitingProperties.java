package com.example.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for IP-based rate limiting.
 *
 * <p>Defaults are conservative and can be overridden via environment variables, e.g.:
 * <ul>
 *   <li>{@code APP_RATE_LIMIT_ENABLED=true}</li>
 *   <li>{@code APP_RATE_LIMIT_CAPACITY=60}</li>
 *   <li>{@code APP_RATE_LIMIT_REFILL_TOKENS=60}</li>
 *   <li>{@code APP_RATE_LIMIT_REFILL_DURATION=PT1M}</li>
 *   <li>{@code APP_RATE_LIMIT_MAX_BUCKETS=10000}</li>
 *   <li>{@code APP_RATE_LIMIT_BUCKET_EXPIRE_AFTER_ACCESS=PT15M}</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitingProperties(
    boolean enabled,
    long capacity,
    long refillTokens,
    java.time.Duration refillDuration,
    long maxBuckets,
    java.time.Duration bucketExpireAfterAccess
) {
  public RateLimitingProperties {
    if (capacity <= 0 || refillTokens <= 0) {
      throw new IllegalArgumentException("Rate limit capacity/refillTokens must be > 0");
    }
    if (refillDuration == null || refillDuration.isNegative() || refillDuration.isZero()) {
      throw new IllegalArgumentException("Rate limit refillDuration must be > 0");
    }
    if (maxBuckets <= 0) {
      throw new IllegalArgumentException("Rate limit maxBuckets must be > 0");
    }
    if (bucketExpireAfterAccess == null || bucketExpireAfterAccess.isNegative() || bucketExpireAfterAccess.isZero()) {
      throw new IllegalArgumentException("Rate limit bucketExpireAfterAccess must be > 0");
    }

  }

  public static RateLimitingProperties defaults() {
    return new RateLimitingProperties(true, 12, 120, java.time.Duration.ofMinutes(1), 10_000, java.time.Duration.ofMinutes(15));
  }
}
