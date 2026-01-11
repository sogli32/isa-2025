package com.example.backend.services;

import com.example.backend.limiter.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minut

    private final RateLimiter rateLimiter = new RateLimiter(MAX_ATTEMPTS, WINDOW_MS);

    public boolean isBlocked(String ip) {
        return !rateLimiter.tryConsume(ip);
    }

    public void loginSucceeded(String ip) {
        rateLimiter.reset(ip);
    }
}
