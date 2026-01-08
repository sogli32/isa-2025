package com.example.backend.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minut

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Attempt attempt = attempts.get(ip);
        if (attempt == null) {
            return false;
        }
        if (System.currentTimeMillis() - attempt.firstAttemptTime > WINDOW_MS) {
            attempts.remove(ip);
            return false;
        }

        return attempt.count >= MAX_ATTEMPTS;
    }

    public void loginFailed(String ip) {
        attempts.compute(ip, (key, value) -> {
            long now = System.currentTimeMillis();

            if (value == null || now - value.firstAttemptTime > WINDOW_MS) {
                return new Attempt(1, now);
            }

            value.count++;
            return value;
        });
    }

    public void loginSucceeded(String ip) {
        attempts.remove(ip);
    }

    private static class Attempt {
        int count;
        long firstAttemptTime;

        Attempt(int count, long firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
    }
}
