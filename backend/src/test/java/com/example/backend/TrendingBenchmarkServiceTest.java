package com.example.backend;

import com.example.backend.dto.TrendingBenchmarkReport;
import com.example.backend.dto.TrendingBenchmarkResult;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.services.PopularityCalculationService;
import com.example.backend.services.TrendingBenchmarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test za TrendingBenchmarkService.
 * 
 * Ovaj test demonstrira merenje performansi različitih trending strategija
 * i generiše izveštaj koji se može koristiti za dokumentaciju.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TrendingBenchmarkServiceTest {
    
    @Autowired
    private TrendingBenchmarkService benchmarkService;
    
    @Autowired
    private VideoRepository videoRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VideoLikeRepository videoLikeRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PopularityCalculationService popularityCalculationService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Kreiraj test korisnika sa svim obaveznim poljima
        testUser = new User();
        testUser.setEmail("benchmark@test.com");
        testUser.setUsername("benchmarkuser");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address 123");
        testUser.setRole("USER");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
        
        // Kreiraj test videe sa različitim view count-ima
        createTestVideos(50);
        
        // Ažuriraj popularity score za sve videe
        popularityCalculationService.updateAllPopularityScores();
    }
    
    private void createTestVideos(int count) {
        List<Video> videos = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Video video = new Video();
            video.setTitle("Test Video " + i);
            video.setDescription("Description for video " + i);
            video.setTags("test,benchmark");
            video.setThumbnailPath("/thumbnails/test" + i + ".jpg");
            video.setVideoPath("/videos/test" + i + ".mp4");
            video.setUser(testUser);
            video.setViewCount((long) (Math.random() * 10000));
            video.setCreatedAt(LocalDateTime.now().minusDays((long) (Math.random() * 30)));
            videos.add(video);
        }
        
        videoRepository.saveAll(videos);
    }
    
    @Test
    void testBenchmarkReturnsValidReport() {
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(20, 10);
        
        assertNotNull(report);
        assertNotNull(report.getTimestamp());
        assertTrue(report.getTotalVideosInDatabase() >= 50);
        assertEquals(20, report.getIterationsPerStrategy());
        assertNotNull(report.getResults());
        assertEquals(3, report.getResults().size()); // 3 strategije
        assertNotNull(report.getOptimalStrategy());
        assertNotNull(report.getRecommendation());
    }
    
    @Test
    void testAllStrategiesAreTested() {
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(10, 10);
        
        List<String> strategyNames = report.getResults().stream()
                .map(TrendingBenchmarkResult::getStrategyName)
                .toList();
        
        assertTrue(strategyNames.contains("REAL_TIME"));
        assertTrue(strategyNames.contains("CACHED"));
        assertTrue(strategyNames.contains("HYBRID"));
    }
    
    @Test
    void testStatisticsAreCalculated() {
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(50, 10);
        
        for (TrendingBenchmarkResult result : report.getResults()) {
            assertTrue(result.getAvgResponseTimeMs() > 0);
            assertTrue(result.getMinResponseTimeMs() > 0);
            assertTrue(result.getMaxResponseTimeMs() >= result.getMinResponseTimeMs());
            assertTrue(result.getMedianResponseTimeMs() > 0);
            assertTrue(result.getP95ResponseTimeMs() >= result.getMedianResponseTimeMs());
            assertTrue(result.getP99ResponseTimeMs() >= result.getP95ResponseTimeMs());
            assertTrue(result.getStandardDeviation() >= 0);
            assertEquals(50, result.getIterations());
        }
    }
    
    @Test
    void testCachedStrategyIsFasterThanRealTime() {
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(50, 20);
        
        TrendingBenchmarkResult realTime = report.getResults().stream()
                .filter(r -> "REAL_TIME".equals(r.getStrategyName()))
                .findFirst()
                .orElseThrow();
        
        TrendingBenchmarkResult cached = report.getResults().stream()
                .filter(r -> "CACHED".equals(r.getStrategyName()))
                .findFirst()
                .orElseThrow();
        
        // CACHED bi trebao biti brži (manji avg response time)
        assertTrue(cached.getAvgResponseTimeMs() < realTime.getAvgResponseTimeMs(),
                "CACHED (" + cached.getAvgResponseTimeMs() + "ms) should be faster than REAL_TIME (" 
                        + realTime.getAvgResponseTimeMs() + "ms)");
        
        System.out.println("\n========== BENCHMARK REZULTATI ==========");
        System.out.println("Broj videa u bazi: " + report.getTotalVideosInDatabase());
        System.out.println("Broj iteracija: " + report.getIterationsPerStrategy());
        System.out.println();
        
        for (TrendingBenchmarkResult result : report.getResults()) {
            System.out.println("--- " + result.getStrategyName() + " ---");
            System.out.println("  Opis: " + result.getDescription());
            System.out.println("  Avg: " + result.getAvgResponseTimeMs() + " ms");
            System.out.println("  Min: " + result.getMinResponseTimeMs() + " ms");
            System.out.println("  Max: " + result.getMaxResponseTimeMs() + " ms");
            System.out.println("  Median: " + result.getMedianResponseTimeMs() + " ms");
            System.out.println("  P95: " + result.getP95ResponseTimeMs() + " ms");
            System.out.println("  P99: " + result.getP99ResponseTimeMs() + " ms");
            System.out.println("  StdDev: " + result.getStandardDeviation() + " ms");
            System.out.println();
        }
        
        System.out.println("OPTIMALNA STRATEGIJA: " + report.getOptimalStrategy());
        System.out.println("PREPORUKA: " + report.getRecommendation());
        System.out.println("==========================================\n");
    }
    
    @Test
    void testResponseTimesAreRecordedForGraphing() {
        TrendingBenchmarkReport report = benchmarkService.runBenchmark(30, 10);
        
        for (TrendingBenchmarkResult result : report.getResults()) {
            assertNotNull(result.getAllResponseTimes());
            assertEquals(30, result.getAllResponseTimes().size());
            
            // Svi response time-ovi moraju biti pozitivni
            assertTrue(result.getAllResponseTimes().stream().allMatch(t -> t > 0));
        }
    }
}