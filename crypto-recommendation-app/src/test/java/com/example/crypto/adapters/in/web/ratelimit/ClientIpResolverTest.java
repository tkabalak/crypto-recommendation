package com.example.crypto.adapters.in.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for client IP resolution.
 */
class ClientIpResolverTest {

  @Test
  void shouldPreferXForwardedForFirstIp() {
    HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
    Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
    Mockito.when(req.getRemoteAddr()).thenReturn("9.9.9.9");
    assertThat(ClientIpResolver.resolve(req)).isEqualTo("1.2.3.4");
  }

  @Test
  void shouldFallbackToRemoteAddr() {
    HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
    Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn(null);
    Mockito.when(req.getRemoteAddr()).thenReturn("9.9.9.9");
    assertThat(ClientIpResolver.resolve(req)).isEqualTo("9.9.9.9");
  }
}
