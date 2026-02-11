package com.example.backend;

import com.example.backend.model.EtlPipelineResult;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.model.VideoView;
import com.example.backend.repository.EtlPipelineResultRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.repository.VideoViewRepository;
import com.example.backend.services.EtlPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EtlPipelineIntegrationTest {

    @Autowired
    private EtlPipelineService etlPipelineService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private EtlPipelineResultRepository etlPipelineResultRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("etltest@example.com");
        testUser.setPassword("password");
        testUser.setUsername("etltestuser");
        testUser.setFirstName("ETL");
        testUser.setLastName("Test");
        testUser.setAddress("Test Address 123");
        testUser.setRole("USER");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testRunEtlPipeline_FullFlow_WithMultipleVideos() {
        // Given - 3 videa sa razlicitim brojem pregleda
        Video v1 = createVideo("Popular Video");
        Video v2 = createVideo("Medium Video");
        Video v3 = createVideo("Unpopular Video");

        // Dodaj preglede: v1 ima najvise, v3 najmanje
        addViews(v1, 10, 0);  // 10 pregleda danas
        addViews(v2, 5, 0);   // 5 pregleda danas
        addViews(v3, 2, 0);   // 2 pregleda danas

        // When
        etlPipelineService.runEtlPipeline();

        // Then
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());

        EtlPipelineResult etlResult = result.get();
        assertNotNull(etlResult.getExecutedAt());

        // Top 1: Popular Video (10 * 7 = 70)
        assertNotNull(etlResult.getVideo1());
        assertEquals(v1.getId(), etlResult.getVideo1().getId());
        assertEquals(70.0, etlResult.getScore1(), 0.01);

        // Top 2: Medium Video (5 * 7 = 35)
        assertNotNull(etlResult.getVideo2());
        assertEquals(v2.getId(), etlResult.getVideo2().getId());
        assertEquals(35.0, etlResult.getScore2(), 0.01);

        // Top 3: Unpopular Video (2 * 7 = 14)
        assertNotNull(etlResult.getVideo3());
        assertEquals(v3.getId(), etlResult.getVideo3().getId());
        assertEquals(14.0, etlResult.getScore3(), 0.01);
    }

    @Test
    void testRunEtlPipeline_WeightedScoring_DifferentDays() {
        // Given - video sa pregledima na razlicite dane
        Video video = createVideo("Multi-day Video");

        addViews(video, 3, 0);  // 3 danas: 3 * 7 = 21
        addViews(video, 2, 1);  // 2 juce: 2 * 7 = 14
        addViews(video, 4, 3);  // 4 pre 3 dana: 4 * 5 = 20
        addViews(video, 1, 6);  // 1 pre 6 dana: 1 * 2 = 2

        // When
        etlPipelineService.runEtlPipeline();

        // Then - ukupno: 21 + 14 + 20 + 2 = 57
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());
        assertEquals(57.0, result.get().getScore1(), 0.01);
    }

    @Test
    void testRunEtlPipeline_NoViews_EmptyResult() {
        // Given - postoje videi ali nema pregleda
        createVideo("No Views Video");

        // When
        etlPipelineService.runEtlPipeline();

        // Then - rezultat postoji ali nema videa
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());
        assertNull(result.get().getVideo1());
        assertNull(result.get().getVideo2());
        assertNull(result.get().getVideo3());
    }

    @Test
    void testRunEtlPipeline_NewerViewsOutweighOlder() {
        // Given
        Video recentVideo = createVideo("Recent Views");
        Video oldVideo = createVideo("Old Views");

        // recentVideo: 3 pregleda danas = 3 * 7 = 21
        addViews(recentVideo, 3, 0);

        // oldVideo: 5 pregleda pre 6 dana = 5 * 2 = 10
        addViews(oldVideo, 5, 6);

        // When
        etlPipelineService.runEtlPipeline();

        // Then - recentVideo (21) je iznad oldVideo (10)
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());
        assertEquals(recentVideo.getId(), result.get().getVideo1().getId());
        assertEquals(21.0, result.get().getScore1(), 0.01);
        assertEquals(oldVideo.getId(), result.get().getVideo2().getId());
        assertEquals(10.0, result.get().getScore2(), 0.01);
    }

    @Test
    void testRunEtlPipeline_OnlyTwoVideosWithViews() {
        // Given - samo 2 videa sa pregledima
        Video v1 = createVideo("Video A");
        Video v2 = createVideo("Video B");
        createVideo("Video C - no views");

        addViews(v1, 5, 0);
        addViews(v2, 3, 0);

        // When
        etlPipelineService.runEtlPipeline();

        // Then
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());
        assertNotNull(result.get().getVideo1());
        assertNotNull(result.get().getVideo2());
        assertNull(result.get().getVideo3()); // treci nema pregleda
    }

    @Test
    void testRunEtlPipeline_MultipleRuns_LatestResultReturned() {
        // Given - pokreni ETL dva puta
        Video video = createVideo("Some Video");
        addViews(video, 5, 0);

        etlPipelineService.runEtlPipeline();

        // Dodaj jos pregleda i pokreni opet
        addViews(video, 3, 0);
        etlPipelineService.runEtlPipeline();

        // Then - findTopByOrderByExecutedAtDesc vraca poslednji rezultat
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());

        // Poslednji ETL run vidi ukupno 8 pregleda danas: 8 * 7 = 56
        assertEquals(56.0, result.get().getScore1(), 0.01);

        // Ukupno 2 rezultata u bazi
        List<EtlPipelineResult> allResults = etlPipelineResultRepository.findAll();
        assertEquals(2, allResults.size());
    }

    @Test
    void testRunEtlPipeline_MoreThan3Videos_OnlyTop3Stored() {
        // Given - 5 videa
        Video v1 = createVideo("Video 1");
        Video v2 = createVideo("Video 2");
        Video v3 = createVideo("Video 3");
        Video v4 = createVideo("Video 4");
        Video v5 = createVideo("Video 5");

        addViews(v1, 1, 0);   // score: 7
        addViews(v2, 10, 0);  // score: 70
        addViews(v3, 5, 0);   // score: 35
        addViews(v4, 3, 0);   // score: 21
        addViews(v5, 8, 0);   // score: 56

        // When
        etlPipelineService.runEtlPipeline();

        // Then - top 3: v2 (70), v5 (56), v3 (35)
        Optional<EtlPipelineResult> result = etlPipelineResultRepository.findTopByOrderByExecutedAtDesc();
        assertTrue(result.isPresent());

        EtlPipelineResult etlResult = result.get();
        assertEquals(v2.getId(), etlResult.getVideo1().getId());
        assertEquals(70.0, etlResult.getScore1(), 0.01);
        assertEquals(v5.getId(), etlResult.getVideo2().getId());
        assertEquals(56.0, etlResult.getScore2(), 0.01);
        assertEquals(v3.getId(), etlResult.getVideo3().getId());
        assertEquals(35.0, etlResult.getScore3(), 0.01);
    }

    // ===== Helper metode =====

    private Video createVideo(String title) {
        Video video = new Video(
                title,
                "Description",
                "tags",
                "thumbnail.jpg",
                "video.mp4",
                testUser,
                null
        );
        video.setViewCount(0L);
        return videoRepository.save(video);
    }

    /**
     * Dodaje odredjeni broj pregleda za video, postavljajuci viewedAt na pre daysAgo dana.
     */
    private void addViews(Video video, int count, int daysAgo) {
        for (int i = 0; i < count; i++) {
            VideoView view = new VideoView();
            view.setVideo(video);
            view.setViewedAt(LocalDateTime.now().minusDays(daysAgo).minusMinutes(i));
            videoViewRepository.save(view);
        }
    }
}
