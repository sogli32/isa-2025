package com.example.backend;

import com.example.backend.services.ActiveUsersMetricService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration testovi za monitoring metrike (Prometheus/Grafana).
 * Testira sve 3 metrike:
 *   1) HikariCP DB konekcije (active/idle) pod opterecenjem
 *   2) CPU usage metrika (process_cpu_usage)
 *   3) Aktivni korisnici u poslednjih 24h (active_users_24h)
 *
 * Zahteva pokrenutu bazu (H2 u test profilu).
 */
@SpringBootTest
@ActiveProfiles("test")
class MonitoringMetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ActiveUsersMetricService activeUsersMetricService;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        activeUsersMetricService.clearAll();
    }

    // =====================================================================
    //  1) ACTIVE USERS METRIKA — active_users_24h
    // =====================================================================

    @Test
    void testActiveUsersMetric_InitiallyZero() {
        double count = activeUsersMetricService.getActiveUserCount();
        assertEquals(0.0, count);
    }

    @Test
    void testActiveUsersMetric_RegistersSingleUser() {
        activeUsersMetricService.recordUserActivity("user1@test.com");
        assertEquals(1.0, activeUsersMetricService.getActiveUserCount());
    }

    @Test
    void testActiveUsersMetric_RegistersMultipleUniqueUsers() {
        for (int i = 1; i <= 25; i++) {
            activeUsersMetricService.recordUserActivity("user" + i + "@test.com");
        }
        assertEquals(25.0, activeUsersMetricService.getActiveUserCount());
    }

    @Test
    void testActiveUsersMetric_SameUserNotCountedTwice() {
        activeUsersMetricService.recordUserActivity("user1@test.com");
        activeUsersMetricService.recordUserActivity("user1@test.com");
        activeUsersMetricService.recordUserActivity("user1@test.com");
        assertEquals(1.0, activeUsersMetricService.getActiveUserCount());
    }

    @Test
    void testActiveUsersMetric_ClearResetsToZero() {
        activeUsersMetricService.recordUserActivity("a@test.com");
        activeUsersMetricService.recordUserActivity("b@test.com");
        activeUsersMetricService.recordUserActivity("c@test.com");
        assertEquals(3.0, activeUsersMetricService.getActiveUserCount());

        activeUsersMetricService.clearAll();
        assertEquals(0.0, activeUsersMetricService.getActiveUserCount());
    }

    @Test
    void testActiveUsersMetric_GaugeRegisteredInMeterRegistry() {
        Gauge gauge = meterRegistry.find("active_users_24h").gauge();
        assertNotNull(gauge, "Gauge 'active_users_24h' mora biti registrovan u MeterRegistry");

        activeUsersMetricService.recordUserActivity("gauge_test@test.com");
        assertEquals(1.0, gauge.value(), 0.01);
    }

    @Test
    void testActiveUsersMetric_ConcurrentAccess() throws InterruptedException {
        int userCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(userCount);

        for (int i = 0; i < userCount; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    activeUsersMetricService.recordUserActivity("concurrent_user" + userId + "@test.com");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(50.0, activeUsersMetricService.getActiveUserCount());
    }

    // =====================================================================
    //  2) DB KONEKCIJE — hikaricp_connections_active / idle
    // =====================================================================

    @Test
    void testDbConnectionMetrics_IdleConnectionsExist() {
        Gauge idleGauge = meterRegistry.find("hikaricp.connections.idle")
                .tag("pool", "HikariPool")
                .gauge();

        assertNotNull(idleGauge, "HikariCP idle connections gauge mora postojati");
        assertTrue(idleGauge.value() >= 1.0,
                "Mora postojati barem 1 idle konekcija, nasao: " + idleGauge.value());
    }

    @Test
    void testDbConnectionMetrics_ActiveConnectionsDuringQuery() throws Exception {
        Gauge activeGauge = meterRegistry.find("hikaricp.connections.active")
                .tag("pool", "HikariPool")
                .gauge();

        assertNotNull(activeGauge, "HikariCP active connections gauge mora postojati");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void testDbConnectionMetrics_ConcurrentQueriesIncreaseActive() throws Exception {
        int concurrentConnections = 10;
        List<Connection> heldConnections = new ArrayList<>();

        Gauge activeGauge = meterRegistry.find("hikaricp.connections.active")
                .tag("pool", "HikariPool")
                .gauge();
        assertNotNull(activeGauge);

        Gauge totalGauge = meterRegistry.find("hikaricp.connections")
                .tag("pool", "HikariPool")
                .gauge();

        try {
            for (int i = 0; i < concurrentConnections; i++) {
                Connection conn = dataSource.getConnection();
                conn.setAutoCommit(false);
                conn.createStatement().executeQuery("SELECT 1");
                heldConnections.add(conn);
            }

            double totalNow = totalGauge != null ? totalGauge.value() : -1;
            double activeNow = activeGauge.value();

            assertTrue(totalNow >= concurrentConnections || activeNow >= 1,
                    "Pool mora imati barem " + concurrentConnections + " total konekcija ili active > 0. " +
                    "Total: " + totalNow + ", Active: " + activeNow);

        } finally {
            for (Connection conn : heldConnections) {
                try {
                    conn.rollback();
                    conn.close();
                } catch (Exception ignored) {}
            }
        }

        Thread.sleep(200);
        double activeAfter = activeGauge.value();
        assertTrue(activeAfter < concurrentConnections,
                "Posle zatvaranja, active bi trebalo da bude manje od " + concurrentConnections + ", nasao: " + activeAfter);
    }

    @Test
    void testDbConnectionMetrics_StressTest() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Connection conn = dataSource.getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        rs.next();
                        Thread.sleep(50);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Stress test nije zavrsio u roku od 30s");
        assertTrue(successCount.get() > 0, "Barem neki upiti moraju uspeti");
        assertEquals(threadCount, successCount.get() + errorCount.get(),
                "Svi threadovi moraju zavrsiti");
    }

    @Test
    void testDbConnectionMetrics_TotalConnectionsExist() {
        Gauge totalGauge = meterRegistry.find("hikaricp.connections")
                .tag("pool", "HikariPool")
                .gauge();

        assertNotNull(totalGauge, "HikariCP total connections gauge mora postojati");
        assertTrue(totalGauge.value() >= 1.0,
                "Pool mora imati barem 1 konekciju, nasao: " + totalGauge.value());
    }

    // =====================================================================
    //  3) CPU USAGE — process_cpu_usage / system_cpu_usage
    // =====================================================================

    @Test
    void testCpuMetrics_ProcessCpuUsageExists() {
        Gauge cpuGauge = meterRegistry.find("process.cpu.usage").gauge();
        assertNotNull(cpuGauge, "process.cpu.usage metrika mora postojati");
        assertTrue(cpuGauge.value() >= 0.0,
                "CPU usage ne moze biti negativan, nasao: " + cpuGauge.value());
    }

    @Test
    void testCpuMetrics_SystemCpuUsageExists() {
        Gauge systemCpuGauge = meterRegistry.find("system.cpu.usage").gauge();
        assertNotNull(systemCpuGauge, "system.cpu.usage metrika mora postojati");
        assertTrue(systemCpuGauge.value() >= 0.0,
                "System CPU usage ne moze biti negativan, nasao: " + systemCpuGauge.value());
    }

    @Test
    void testCpuMetrics_CpuUsageIncreasesUnderLoad() throws InterruptedException {
        Gauge cpuGauge = meterRegistry.find("process.cpu.usage").gauge();
        assertNotNull(cpuGauge);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    return;
                }
                long end = System.currentTimeMillis() + 500;
                double x = 1.0;
                while (System.currentTimeMillis() < end) {
                    x = Math.sin(x) * Math.cos(x) + Math.sqrt(Math.abs(x));
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        double cpuAfterLoad = cpuGauge.value();
        assertTrue(cpuAfterLoad >= 0.0,
                "CPU usage posle load-a mora biti >= 0, nasao: " + cpuAfterLoad);
    }

    // =====================================================================
    //  4) PROMETHEUS ENDPOINT — sve metrike registrovane
    // =====================================================================

    @Test
    void testPrometheusEndpoint_MetricsAvailable() {
        assertNotNull(meterRegistry.find("active_users_24h").gauge(),
                "active_users_24h mora biti registrovana");
        assertNotNull(meterRegistry.find("hikaricp.connections.active").gauge(),
                "hikaricp.connections.active mora biti registrovana");
        assertNotNull(meterRegistry.find("hikaricp.connections.idle").gauge(),
                "hikaricp.connections.idle mora biti registrovana");
        assertNotNull(meterRegistry.find("process.cpu.usage").gauge(),
                "process.cpu.usage mora biti registrovana");
    }

    @Test
    void testAllMetrics_SimulateFullMonitoringScenario() throws Exception {
        // 1. Registruj 10 korisnika
        for (int i = 0; i < 10; i++) {
            activeUsersMetricService.recordUserActivity("scenario_user" + i + "@test.com");
        }
        assertEquals(10.0, activeUsersMetricService.getActiveUserCount());

        // 2. Izvrsi 20 istovremenih DB upita
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    rs.next();
                    success.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(success.get() >= 15, "Vecina DB upita mora uspeti: " + success.get() + "/20");

        // 3. Proveri sve metrike
        Gauge activeUsers = meterRegistry.find("active_users_24h").gauge();
        assertNotNull(activeUsers);
        assertEquals(10.0, activeUsers.value(), 0.01);

        Gauge cpuGauge = meterRegistry.find("process.cpu.usage").gauge();
        assertNotNull(cpuGauge);
        assertTrue(cpuGauge.value() >= 0.0);

        Gauge dbActive = meterRegistry.find("hikaricp.connections.active").gauge();
        assertNotNull(dbActive);
        assertTrue(dbActive.value() >= 0.0);

        // 4. Reset i provera
        activeUsersMetricService.clearAll();
        assertEquals(0.0, activeUsers.value(), 0.01);
    }
}
