package com.example.backend.controller;

import com.example.backend.services.ActiveUsersMetricService;
import com.example.backend.repository.VideoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kontroler za pokretanje load testova koji demonstriraju monitoring metrike.
 * Pokreni test pa gledaj Grafana dashboard u realnom vremenu.
 *
 * Endpointi:
 *   GET /api/load-test/db-connections?requests=500&threads=50
 *   GET /api/load-test/active-users?userCount=15
 *   GET /api/load-test/full?requests=500&threads=50&userCount=10
 */
@RestController
@RequestMapping("/api/load-test")
@CrossOrigin(origins = "http://localhost:4200")
public class MonitoringLoadTestController {

    private final VideoRepository videoRepository;
    private final ActiveUsersMetricService activeUsersMetricService;

    public MonitoringLoadTestController(VideoRepository videoRepository,
                                        ActiveUsersMetricService activeUsersMetricService) {
        this.videoRepository = videoRepository;
        this.activeUsersMetricService = activeUsersMetricService;
    }

    /**
     * Test 1: Broj aktivnih/idle konekcija ka bazi pod opterecenjem.
     * Salje mnogo konkurentnih upita ka bazi da bi HikariCP morao da otvori vise konekcija.
     *
     * Primer: GET /api/load-test/db-connections?requests=500&threads=50
     * Gledaj u Grafani: hikaricp_connections_active i hikaricp_connections_idle
     */
    @GetMapping("/db-connections")
    public ResponseEntity<Map<String, Object>> testDbConnections(
            @RequestParam(defaultValue = "500") int requests,
            @RequestParam(defaultValue = "50") int threads
    ) throws InterruptedException {
        requests = Math.min(requests, 5000);
        threads = Math.min(threads, 200);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Svi threadovi krecu istovremeno
                    // Upit ka bazi — svaki zahtev koristi jednu konekciju
                    videoRepository.findAllByOrderByCreatedAtDesc();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Pokreni sve odjednom
        startLatch.countDown();
        doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;
        double requestsPerSecond = (requests * 1000.0) / duration;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("test", "DB Connection Pool Load Test");
        result.put("totalRequests", requests);
        result.put("threads", threads);
        result.put("successCount", successCount.get());
        result.put("errorCount", errorCount.get());
        result.put("durationMs", duration);
        result.put("requestsPerSecond", Math.round(requestsPerSecond * 100.0) / 100.0);
        result.put("tip", "Pogledaj Grafanu: hikaricp_connections_active i hikaricp_connections_idle");

        return ResponseEntity.ok(result);
    }

    /**
     * Test 2: Simulira vise aktivnih korisnika za metriku active_users_24h.
     * Registruje N fake korisnika u ActiveUsersMetricService.
     *
     * Primer: GET /api/load-test/active-users?userCount=15
     * Gledaj u Grafani: active_users_24h
     */
    @GetMapping("/active-users")
    public ResponseEntity<Map<String, Object>> testActiveUsers(
            @RequestParam(defaultValue = "10") int userCount
    ) {
        userCount = Math.min(userCount, 100);

        List<String> simulatedUsers = new ArrayList<>();
        for (int i = 1; i <= userCount; i++) {
            String fakeEmail = "loadtest_user" + i + "@test.com";
            activeUsersMetricService.recordUserActivity(fakeEmail);
            simulatedUsers.add(fakeEmail);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("test", "Active Users Simulation");
        result.put("simulatedUsers", userCount);
        result.put("currentActiveUsers", (int) activeUsersMetricService.getActiveUserCount());
        result.put("tip", "Pogledaj Grafanu: active_users_24h — trebalo bi da pokaze " + userCount + "+ korisnika");

        return ResponseEntity.ok(result);
    }

    /**
     * Test 3: FULL load test — kombinuje sve metrike:
     * - DB konekcije pod opterecenjem (200+ req/s)
     * - CPU skok zbog velikog broja threadova
     * - Simulacija aktivnih korisnika
     *
     * Primer: GET /api/load-test/full?requests=1000&threads=100&userCount=20
     * Gledaj u Grafani: SVE metrike se menjaju!
     */
    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> fullLoadTest(
            @RequestParam(defaultValue = "1000") int requests,
            @RequestParam(defaultValue = "100") int threads,
            @RequestParam(defaultValue = "20") int userCount
    ) throws InterruptedException {
        requests = Math.min(requests, 5000);
        threads = Math.min(threads, 200);
        userCount = Math.min(userCount, 100);

        // 1. Simuliraj aktivne korisnike
        for (int i = 1; i <= userCount; i++) {
            activeUsersMetricService.recordUserActivity("fulltest_user" + i + "@test.com");
        }

        // 2. Pokreni masivni DB load test (ovo ce podici i CPU i DB connections)
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requests; i++) {
            final int requestNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Razliciti upiti da bude realisticniji load
                    if (requestNum % 3 == 0) {
                        videoRepository.findAllByOrderByCreatedAtDesc();
                    } else if (requestNum % 3 == 1) {
                        videoRepository.count();
                    } else {
                        videoRepository.findTrendingVideos(
                                org.springframework.data.domain.PageRequest.of(0, 20));
                    }

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;
        double requestsPerSecond = (requests * 1000.0) / duration;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("test", "FULL Monitoring Load Test");
        result.put("totalRequests", requests);
        result.put("threads", threads);
        result.put("simulatedActiveUsers", userCount);
        result.put("successCount", successCount.get());
        result.put("errorCount", errorCount.get());
        result.put("durationMs", duration);
        result.put("requestsPerSecond", Math.round(requestsPerSecond * 100.0) / 100.0);
        result.put("currentActiveUsers", (int) activeUsersMetricService.getActiveUserCount());
        result.put("metrike", Map.of(
                "dbConnections", "hikaricp_connections_active / hikaricp_connections_idle",
                "cpu", "system_cpu_usage",
                "activeUsers", "active_users_24h"
        ));
        result.put("tip", "Pogledaj Grafanu ODMAH — sve 3 metrike bi trebale da pokazu promene!");

        return ResponseEntity.ok(result);
    }

    /**
     * Reset: Brise sve simulirane korisnike iz metrike.
     * Posle ovoga active_users_24h se vraca na 0 (ili na broj pravih korisnika).
     *
     * Primer: GET /api/load-test/reset-users
     */
    @GetMapping("/reset-users")
    public ResponseEntity<Map<String, Object>> resetUsers() {
        activeUsersMetricService.clearAll();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Svi simulirani korisnici obrisani");
        result.put("currentActiveUsers", (int) activeUsersMetricService.getActiveUserCount());

        return ResponseEntity.ok(result);
    }
}
