package com.example.backend.limiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final int maxTokens;
    private final long refillIntervalMs;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxTokens, long refillIntervalMs) {
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
    }

    public boolean tryConsume(String key) {
        return buckets.computeIfAbsent(key, k -> new TokenBucket(maxTokens, refillIntervalMs))
                .tryConsume();
    }

    public void reset(String key) {
        TokenBucket bucket = buckets.get(key);
        if (bucket != null) {
            bucket.reset();
        }
    }
}
