package com.example.backend.limiter;

public class TokenBucket {

    private final int capacity;
    private final long refillIntervalMs;
    private double tokens;
    private long lastRefillTime;

    public TokenBucket(int capacity, long refillIntervalMs) {
        this.capacity = capacity;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        double newTokens = ((now - lastRefillTime) / (double) refillIntervalMs) * capacity;
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefillTime = now;
    }

    public synchronized void reset() {
        tokens = capacity;
        lastRefillTime = System.currentTimeMillis();
    }
}
