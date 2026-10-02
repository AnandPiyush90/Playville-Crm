package com.playville.crm.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetRateLimiter {

    private static final long ACCOUNT_WINDOW_MS = 60_000L;
    private static final long IP_WINDOW_MS = 300_000L;
    private static final int MAX_IP_REQUESTS = 5;

    private final Map<String, Long> lastAccountRequest = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> ipRequestHistory = new ConcurrentHashMap<>();

    public boolean isThrottled(String email, String ipAddress) {
        String normalizedEmail = normalize(email);
        String normalizedIp = normalize(ipAddress);

        long now = Instant.now().toEpochMilli();

        if (normalizedEmail != null) {
            Long last = lastAccountRequest.get(normalizedEmail);
            if (last != null && now - last < ACCOUNT_WINDOW_MS) {
                return true;
            }
            lastAccountRequest.put(normalizedEmail, now);
        }

        if (normalizedIp != null) {
            Deque<Long> history = ipRequestHistory.computeIfAbsent(normalizedIp, key -> new ArrayDeque<>());
            long cutoff = now - IP_WINDOW_MS;
            while (!history.isEmpty() && history.peekFirst() < cutoff) {
                history.pollFirst();
            }
            if (history.size() >= MAX_IP_REQUESTS) {
                return true;
            }
            history.addLast(now);
        }

        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase();
    }
}
