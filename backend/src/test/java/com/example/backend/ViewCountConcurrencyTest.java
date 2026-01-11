package com.example.backend;

import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test za demonstriranje konzistentnosti brojača pregleda
 * pri istovremenom pristupu istom videu od strane više korisnika
 * 
 * NAPOMENA: Specifikacija kaže "3.7 (bez transakcije)"
 * Konzistentnost se postiže atomskom UPDATE operacijom na bazi,
 * bez eksplicitne @Transactional anotacije.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ViewCountConcurrencyTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private Video testVideo;
    private User testUser;

    @BeforeEach
    public void setup() {
        // Kreiranje test korisnika
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address");
        testUser.setRole("USER");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Kreiranje test videa
        testVideo = new Video();
        testVideo.setTitle("Test Video");
        testVideo.setDescription("Test Description");
        testVideo.setTags("test,video");
        testVideo.setThumbnailPath("test-thumb.jpg");
        testVideo.setVideoPath("test-video.mp4");
        testVideo.setUser(testUser);
        testVideo.setCreatedAt(LocalDateTime.now());
        testVideo.setViewCount(0L);
        testVideo = videoRepository.save(testVideo);
    }

    @Test
    public void testConcurrentViewCountIncrement() throws InterruptedException, ExecutionException {
        final int THREAD_COUNT = 50; // Broj istovremenih korisnika
        final Long videoId = testVideo.getId();

        // Thread pool za simulaciju konkurentnih zahteva
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  CONCURRENT VIEW COUNT TEST - BEZ TRANSAKCIJE             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Initial view count: " + testVideo.getViewCount());
        System.out.println("Simulating " + THREAD_COUNT + " concurrent users...");
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Pokretanje THREAD_COUNT threads koji istovremeno pristupaju istom videu
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int userId = i;
            Future<Boolean> future = executorService.submit(() -> {
                try {
                    System.out.println("  → User " + userId + " viewing video...");
                    videoService.incrementViewCount(videoId);
                    return true;
                } catch (Exception e) {
                    System.err.println("  ✗ User " + userId + " failed: " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }

        // Čekanje da svi thread-ovi završe
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();

        // Osvežavanje videa iz baze
        Video updatedVideo = videoRepository.findById(videoId).orElseThrow();

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST RESULTS                                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("Execution time: " + (endTime - startTime) + " ms");
        System.out.println("Successful increments: " + successCount + "/" + THREAD_COUNT);
        System.out.println("Final view count: " + updatedVideo.getViewCount());
        System.out.println("Expected view count: " + THREAD_COUNT);
        System.out.println();

        // Provera konzistentnosti
        assertEquals(THREAD_COUNT, updatedVideo.getViewCount(),
                "View count should be exactly " + THREAD_COUNT + " after " + THREAD_COUNT + " concurrent views");

        System.out.println("✓ Test passed - View count is CONSISTENT!");
        System.out.println("✓ Atomska UPDATE operacija radi bez @Transactional");
        System.out.println();
    }

    @Test
    public void testSequentialViewCountIncrement() {
        final int VIEW_COUNT = 10;
        final Long videoId = testVideo.getId();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  SEQUENTIAL VIEW COUNT TEST                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("Initial view count: " + testVideo.getViewCount());

        for (int i = 0; i < VIEW_COUNT; i++) {
            videoService.incrementViewCount(videoId);
            System.out.println("  Increment " + (i + 1) + " completed");
        }

        Video updatedVideo = videoRepository.findById(videoId).orElseThrow();

        System.out.println();
        System.out.println("Final view count: " + updatedVideo.getViewCount());
        System.out.println("Expected: " + VIEW_COUNT);

        assertEquals(VIEW_COUNT, updatedVideo.getViewCount(),
                "View count should be exactly " + VIEW_COUNT);

        System.out.println("✓ Sequential test passed!");
        System.out.println();
    }

    @Test
    public void testViewCountIncrementWithNonExistentVideo() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  NON-EXISTENT VIDEO TEST                                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        assertThrows(IllegalArgumentException.class, () -> {
            videoService.incrementViewCount(99999L);
        });

        System.out.println("✓ Exception correctly thrown for non-existent video!");
        System.out.println();
    }

    @Test
    public void testHighConcurrency() throws InterruptedException, ExecutionException {
        final int THREAD_COUNT = 100; // Ekstremna konkurencija
        final Long videoId = testVideo.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  HIGH CONCURRENCY TEST - 100 THREADS                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Svi thread-ovi čekaju da startuju u isto vreme
                    videoService.incrementViewCount(videoId);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        Video updatedVideo = videoRepository.findById(videoId).orElseThrow();

        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("Success: " + successCount);
        System.out.println("Final count: " + updatedVideo.getViewCount());
        System.out.println("Time: " + (endTime - startTime) + " ms");

        assertEquals(THREAD_COUNT, updatedVideo.getViewCount());

        System.out.println("✓ High concurrency test passed!");
        System.out.println();
    }
}
