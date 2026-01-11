package com.example.backend.limiter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    @Test
    void testConsumeAndBlock() {
        TokenBucket bucket = new TokenBucket(5, 60_000);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume(), "Token should be available");
        }
        assertFalse(bucket.tryConsume(), "Token should be blocked");
    }

    @Test
    void testRefill() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(5, 100); // refill brzo za test

        for (int i = 0; i < 5; i++) bucket.tryConsume();
        assertFalse(bucket.tryConsume());

        Thread.sleep(150); // čekamo refill
        assertTrue(bucket.tryConsume(), "Token should be refilled");
    }

    @Test
    void testReset() {
        TokenBucket bucket = new TokenBucket(5, 60_000);

        for (int i = 0; i < 5; i++) bucket.tryConsume();
        assertFalse(bucket.tryConsume());

        bucket.reset();
        assertTrue(bucket.tryConsume(), "After reset, token should be available");
    }
}
