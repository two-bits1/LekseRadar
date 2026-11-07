package com.lekseradar.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    /** Часы, которые двигает тест. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-07T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void tryAcquireStopsAtLimitAndReopensAfterWindow() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(clock);
        Duration window = Duration.ofMinutes(10);
        for (int i = 0; i < 3; i++) assertTrue(limiter.tryAcquire("k", 3, window));
        assertFalse(limiter.tryAcquire("k", 3, window));
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        assertTrue(limiter.tryAcquire("k", 3, window));
    }

    @Test
    void countRecordAndReset() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(clock);
        Duration window = Duration.ofMinutes(15);
        limiter.record("a");
        limiter.record("a");
        assertEquals(2, limiter.count("a", window));
        assertEquals(0, limiter.count("b", window));
        limiter.reset("a");
        assertEquals(0, limiter.count("a", window));
    }
}
