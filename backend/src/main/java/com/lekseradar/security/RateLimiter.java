package com.lekseradar.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Счётчик событий в скользящем окне, в памяти одного сервера (для хакатона хватает).
 * Ключ — например {@code login:email:anna@x.no} или {@code ai:user:7}.
 */
@Component
public class RateLimiter {
    private static final int MAX_KEYS = 50_000;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Сколько событий по ключу за последние {@code window}. */
    public int count(String key, Duration window) {
        Deque<Instant> q = hits.get(key);
        if (q == null) return 0;
        synchronized (q) {
            prune(q, window);
            return q.size();
        }
    }

    /** Записать событие. */
    public void record(String key) {
        if (hits.size() > MAX_KEYS) hits.clear();   // защита памяти от потока случайных ключей
        Deque<Instant> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(clock.instant());
        }
    }

    /** Записать событие, если за окно их меньше {@code limit}. false — лимит исчерпан. */
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (hits.size() > MAX_KEYS) hits.clear();
        Deque<Instant> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q, window);
            if (q.size() >= limit) return false;
            q.addLast(clock.instant());
            return true;
        }
    }

    public void reset(String key) {
        hits.remove(key);
    }

    private void prune(Deque<Instant> q, Duration window) {
        Instant from = clock.instant().minus(window);
        while (!q.isEmpty() && q.peekFirst().isBefore(from)) q.pollFirst();
    }
}
