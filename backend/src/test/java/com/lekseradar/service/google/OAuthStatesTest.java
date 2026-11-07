package com.lekseradar.service.google;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class OAuthStatesTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-07T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void ownFreshStateIsValid_survivesRestart() {
        TestClock clock = new TestClock();
        String state = new OAuthStates(SECRET, clock).issue(7L);
        assertTrue(new OAuthStates(SECRET, clock).valid(state, 7L), "другой экземпляр (перезапуск) с тем же секретом принимает");
    }

    @Test
    void otherStudentTamperedExpiredOrOtherServerIsInvalid() {
        TestClock clock = new TestClock();
        OAuthStates states = new OAuthStates(SECRET, clock);
        String state = states.issue(7L);
        assertFalse(states.valid(state, 8L));
        assertFalse(states.valid(state.replaceFirst("^7\\.", "8."), 8L));
        assertFalse(states.valid(state + "x", 7L));
        assertFalse(states.valid("garbage", 7L));
        assertFalse(states.valid(null, 7L));
        assertFalse(new OAuthStates("другой-секрет-другой-секрет-другой-12345", clock).valid(state, 7L));
        clock.now = clock.now.plus(Duration.ofMinutes(11));
        assertFalse(states.valid(state, 7L));
    }
}
