package com.example.crypto.domain.exception;

/**
 * Thrown when a client exceeds the configured rate limit.
 * Mapped to HTTP 429 (application/problem+json).
 */
public class RateLimitExceededException extends RuntimeException {

    private final String clientIp;

    public RateLimitExceededException(String clientIp) {
        super("Rate limit exceeded");
        this.clientIp = clientIp;
    }

    /**
     * @return client IP used for rate limiting
     */
    public String getClientIp() {
        return clientIp;
    }
}
