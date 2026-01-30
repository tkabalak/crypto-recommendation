package com.example.crypto.adapters.in.web.ratelimit;

import com.example.crypto.config.properties.RateLimitingProperties;
import com.example.crypto.domain.exception.RateLimitExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * Simple IP-based rate limiter implemented as a servlet filter.
 *
 * <p>Uses Bucket4j token buckets and stores buckets in-memory. This is sufficient for a single replica and for
 * test tasks. For multiple replicas, preferable way would be distributed storage (e.g. Redis).
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IpRateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingProperties props;
    private final Cache<String, Bucket> buckets;
    private final Counter allowed;
    private final Counter blocked;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private static final Logger log = LoggerFactory.getLogger(IpRateLimitingFilter.class);
    private final ObjectMapper objectMapper;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public IpRateLimitingFilter(RateLimitingProperties props, MeterRegistry registry, HandlerExceptionResolver handlerExceptionResolver, ObjectMapper objectMapper) {
        this.props = props;

        this.buckets = Caffeine.newBuilder()
                .maximumSize(props.maxBuckets())
                .expireAfterAccess(props.bucketExpireAfterAccess())
                .build();

        this.allowed = Counter.builder("app_rate_limit_requests_total")
                .description("Requests passing the IP rate limiter")
                .tag("outcome", "allowed")
                .register(registry);

        this.blocked = Counter.builder("app_rate_limit_requests_total")
                .description("Requests blocked by the IP rate limiter")
                .tag("outcome", "blocked")
                .register(registry);

        // Low-cardinality gauge with current bucket cache size
        registry.gauge("app_rate_limit_bucket_cache_size", buckets, Cache::estimatedSize);

        this.handlerExceptionResolver = handlerExceptionResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.enabled()) {
            return true;
        }

        var path = request.getRequestURI();

        // Do not rate-limit health probes and swagger/docs
        return matcher.match("/actuator/**", path)
                || matcher.match("/swagger-ui/**", path)
                || matcher.match("/v3/api-docs/**", path)
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = ClientIpResolver.resolve(request);
        Bucket bucket = buckets.get(ip, k -> newBucket(props.capacity(), props.refillTokens(), props.refillDuration()));

        if (bucket.tryConsume(1)) {
            allowed.increment();
            filterChain.doFilter(request, response);

            return;
        }

        blocked.increment();
        log.warn("Rate limit exceeded for ip={} path={}", ip, request.getRequestURI());

        RateLimitExceededException ex = new RateLimitExceededException(ip);

        // Exceptions thrown in a servlet Filter happen before the controller, so we must delegate to
        // Spring's HandlerExceptionResolver to produce problem+json.
        var resolved = handlerExceptionResolver.resolveException(request, response, null, ex);
        if (resolved == null && !response.isCommitted()) {
            var pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");

            pd.setTitle("Rate limit exceeded");
            pd.setType(URI.create("https://test-task.example/problems/rate-limit-exceeded"));
            pd.setProperty("clientIp", ip);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(pd));
        }
    }

    private Bucket newBucket(long capacity, long refillTokens, Duration duration) {
        Refill refill = Refill.intervally(refillTokens, duration);

        Bandwidth limit = Bandwidth.classic(capacity, refill);

        return Bucket.builder().addLimit(limit).build();
    }
}
