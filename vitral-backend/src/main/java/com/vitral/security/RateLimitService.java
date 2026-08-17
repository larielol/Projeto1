package com.vitral.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.vitral.exception.BusinessException;

@Service
public class RateLimitService {

    private static final int MAX_TRACKED_KEYS = 10_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int limit, Duration duration) {
        Instant now = Instant.now();
        AtomicBoolean denied = new AtomicBoolean(false);
        windows.compute(key, (ignored, current) -> {
            Window active = current == null || !current.resetAt().isAfter(now)
                    ? new Window(0, now.plus(duration))
                    : current;
            if (active.count() >= limit) {
                denied.set(true);
                return active;
            }
            return new Window(active.count() + 1, active.resetAt());
        });

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry -> !entry.getValue().resetAt().isAfter(now));
        }
        if (denied.get()) {
            throw new BusinessException(
                    "Muitas tentativas. Aguarde alguns minutos e tente novamente.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void reset(String key) {
        windows.remove(key);
    }

    private record Window(int count, Instant resetAt) {
    }
}
