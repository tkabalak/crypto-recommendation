package com.example.crypto.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.crypto.domain.exception.InvalidDateRangeException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TimeRange}.
 */
class TimeRangeTest {

  @Test
  void of_shouldCreateInclusiveExclusiveRange() {
    TimeRange range = TimeRange.of(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-02"));
    assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-01-03T00:00:00Z"));
  }

  @Test
  void of_fromAfterTo_shouldThrow() {
    assertThatThrownBy(() -> TimeRange.of(LocalDate.parse("2026-01-03"), LocalDate.parse("2026-01-01")))
        .isInstanceOf(InvalidDateRangeException.class);
  }

  @Test
  void singleDay_shouldCreateSingleDayRange() {
    TimeRange range = TimeRange.singleDay(LocalDate.parse("2026-01-05"));
    assertThat(range.fromInclusive()).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"));
    assertThat(range.toExclusive()).isEqualTo(Instant.parse("2026-01-06T00:00:00Z"));
  }
}
