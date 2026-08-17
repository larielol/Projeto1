package com.vitral.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiration) {
        revokedTokens.put(token, expiration);
    }

    public boolean isRevoked(String token) {
        return revokedTokens.containsKey(token);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
