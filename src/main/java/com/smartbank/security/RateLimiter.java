package com.smartbank.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory sliding-window limiter. Fine for one server; resets on restart. */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Counts this call. Returns false if the key already used its limit inside the window. */
    public boolean tryAcquire(String key, int maxHits, Duration window) {
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            long now = System.currentTimeMillis();
            prune(times, now - window.toMillis());
            if (times.size() >= maxHits) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    /** True if the key has already reached its limit. Does not count a hit. */
    public boolean isLimited(String key, int maxHits, Duration window) {
        Deque<Long> times = hits.get(key);
        if (times == null) {
            return false;
        }
        synchronized (times) {
            prune(times, System.currentTimeMillis() - window.toMillis());
            return times.size() >= maxHits;
        }
    }

    /** Records one hit (used for failures). */
    public void record(String key) {
        Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            times.addLast(System.currentTimeMillis());
        }
    }

    public void clear(String key) {
        hits.remove(key);
    }

    public long retryAfterSeconds(String key, Duration window) {
        Deque<Long> times = hits.get(key);
        if (times == null) {
            return 1;
        }
        synchronized (times) {
            Long oldest = times.peekFirst();
            if (oldest == null) {
                return 1;
            }
            long waitMillis = oldest + window.toMillis() - System.currentTimeMillis();
            return Math.max(waitMillis / 1000 + 1, 1);
        }
    }

    private void prune(Deque<Long> times, long cutoff) {
        while (!times.isEmpty() && times.peekFirst() <= cutoff) {
            times.pollFirst();
        }
    }
}