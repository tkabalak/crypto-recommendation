package com.example.crypto.adapters.in.web.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Resolves the client IP address from an HTTP request.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    /**
     * Resolves client IP from {@code X-Forwarded-For} first value or falls back to {@code remoteAddr}.
     */
    public static String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");

        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }
}
