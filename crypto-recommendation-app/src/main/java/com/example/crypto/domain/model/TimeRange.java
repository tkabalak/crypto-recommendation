package com.example.crypto.domain.model;

import com.example.crypto.domain.exception.InvalidDateRangeException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Range is [fromInclusive, toExclusive] in UTC (ts >= from and ts < to).
 */
public record TimeRange(Instant fromInclusive, Instant toExclusive) {

    public static final ZoneId DEFAULT_ZONE = ZoneOffset.UTC;

    public static TimeRange of(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        if (to.isBefore(from)) {
            throw new InvalidDateRangeException(from, to);
        }

        var start = from.atStartOfDay(DEFAULT_ZONE).toInstant();
        var endExclusive = to.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        return new TimeRange(start, endExclusive);
    }

    public static TimeRange singleDay(LocalDate day) {
        Objects.requireNonNull(day, "day");

        var start = day.atStartOfDay(DEFAULT_ZONE).toInstant();
        var endExclusive = day.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        return new TimeRange(start, endExclusive);
    }
}
