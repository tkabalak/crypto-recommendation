package com.example.crypto.observability;

import com.example.crypto.adapters.in.web.ratelimit.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Adds/propagates {@code X-Request-Id} and puts it into MDC for log correlation.
 *
 * <p>Also adds a service instance id to MDC, which helps distinguish logs from different pods.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_SERVICE_INSTANCE_ID = "serviceInstanceId";

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    private final ServiceInstanceId serviceInstanceId;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RequestCorrelationFilter(ServiceInstanceId serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Keep logs clean for probes and preflight.
        String path = request.getRequestURI();

        return matcher.match("/actuator/**", path) || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startNanos = System.nanoTime();
        String requestId = Optional.ofNullable(request.getHeader(HEADER_REQUEST_ID))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        String ip = ClientIpResolver.resolve(request);

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_SERVICE_INSTANCE_ID, serviceInstanceId.id());
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            log.info("Incoming request: {} {} from ip={}", request.getMethod(), request.getRequestURI(), ip);

            filterChain.doFilter(request, response);
        } finally {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;

            log.info("Completed request: {} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), tookMs);

            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_SERVICE_INSTANCE_ID);
        }
    }
}
