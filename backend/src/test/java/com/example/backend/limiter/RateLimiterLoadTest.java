package com.example.backend.limiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RateLimiterLoadTest {

    @Test
    void testHighLoad() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(5, 60_000);
        String ip = "192.168.1.1";
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                boolean allowed = limiter.tryConsume(ip);
                System.out.println("Request allowed: " + allowed);
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
