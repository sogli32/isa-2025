package com.example.backend;

import com.example.backend.controller.EtlController;
import com.example.backend.dto.EtlPopularVideoResponse;
import com.example.backend.model.EtlPipelineResult;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.EtlPipelineResultRepository;
import com.example.backend.services.EtlPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtlControllerTest {

    @Mock
    private EtlPipelineResultRepository etlPipelineResultRepository;

    // Rucno kreiran EtlPipelineService (Mockito ne moze da ga mokuje na Java 25)
    private EtlPipelineService etlPipelineService;

    private EtlController etlController;

    private boolean pipelineWasRun;

    private User testUser;
    private Video video1;
    private Video video2;
    private Video video3;

    @BeforeEach
    void setUp() {
        pipelineWasRun = false;

        // Kreiranje EtlPipelineService sa null zavisnostima
        // jer ga koristimo samo za runPipeline() test
        etlPipelineService = new EtlPipelineService(null, null, null) {
            @Override
            public void runEtlPipeline() {
                pipelineWasRun = true;
            }
        };

        etlController = new EtlController(etlPipelineResultRepository, etlPipelineService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");

        video1 = new Video();
        video1.setId(1L);
        video1.setTitle("Video 1");
        video1.setUser(testUser);
        video1.setViewCount(100L);

        video2 = new Video();
        video2.setId(2L);
        video2.setTitle("Video 2");
        video2.setUser(testUser);
        video2.setViewCount(50L);

        video3 = new Video();
        video3.setId(3L);
        video3.setTitle("Video 3");
        video3.setUser(testUser);
        video3.setViewCount(25L);
    }

    @Test
    void testGetPopularVideos_NoResults_Returns200WithEmptyList() {
        // Given - nema ETL rezultata
        when(etlPipelineResultRepository.findTopByOrderByExecutedAtDesc())
                .thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = etlController.getPopularVideos();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof EtlPopularVideoResponse);

        EtlPopularVideoResponse body = (EtlPopularVideoResponse) response.getBody();
        assertNull(body.getExecutedAt());
        assertNotNull(body.getVideos());
        assertTrue(body.getVideos().isEmpty());
    }

    @Test
    void testGetPopularVideos_WithResults_ReturnsTop3() {
        // Given - postoji ETL rezultat sa 3 videa
        EtlPipelineResult etlResult = new EtlPipelineResult(LocalDateTime.now());
        etlResult.setVideo1(video1);
        etlResult.setScore1(70.0);
        etlResult.setVideo2(video2);
        etlResult.setScore2(35.0);
        etlResult.setVideo3(video3);
        etlResult.setScore3(14.0);

        when(etlPipelineResultRepository.findTopByOrderByExecutedAtDesc())
                .thenReturn(Optional.of(etlResult));

        // When
        ResponseEntity<?> response = etlController.getPopularVideos();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        EtlPopularVideoResponse body = (EtlPopularVideoResponse) response.getBody();
        assertNotNull(body);
        assertNotNull(body.getExecutedAt());
        assertEquals(3, body.getVideos().size());

        // Proveri prvi video
        assertEquals(1L, body.getVideos().get(0).getVideoId());
        assertEquals("Video 1", body.getVideos().get(0).getTitle());
        assertEquals("testuser", body.getVideos().get(0).getUsername());
        assertEquals(70.0, body.getVideos().get(0).getPopularityScore(), 0.01);
        assertEquals(100L, body.getVideos().get(0).getViewCount());

        // Proveri drugi video
        assertEquals(2L, body.getVideos().get(1).getVideoId());
        assertEquals("Video 2", body.getVideos().get(1).getTitle());
        assertEquals(35.0, body.getVideos().get(1).getPopularityScore(), 0.01);

        // Proveri treci video
        assertEquals(3L, body.getVideos().get(2).getVideoId());
        assertEquals("Video 3", body.getVideos().get(2).getTitle());
        assertEquals(14.0, body.getVideos().get(2).getPopularityScore(), 0.01);
    }

    @Test
    void testGetPopularVideos_WithPartialResults_OnlyOneVideo() {
        // Given - ETL rezultat sa samo jednim videom
        EtlPipelineResult etlResult = new EtlPipelineResult(LocalDateTime.now());
        etlResult.setVideo1(video1);
        etlResult.setScore1(42.0);
        // video2 i video3 su null

        when(etlPipelineResultRepository.findTopByOrderByExecutedAtDesc())
                .thenReturn(Optional.of(etlResult));

        // When
        ResponseEntity<?> response = etlController.getPopularVideos();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        EtlPopularVideoResponse body = (EtlPopularVideoResponse) response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getVideos().size());
        assertEquals(1L, body.getVideos().get(0).getVideoId());
        assertEquals(42.0, body.getVideos().get(0).getPopularityScore(), 0.01);
    }

    @Test
    void testGetPopularVideos_WithPartialResults_TwoVideos() {
        // Given - ETL rezultat sa 2 videa
        EtlPipelineResult etlResult = new EtlPipelineResult(LocalDateTime.now());
        etlResult.setVideo1(video1);
        etlResult.setScore1(70.0);
        etlResult.setVideo2(video2);
        etlResult.setScore2(35.0);

        when(etlPipelineResultRepository.findTopByOrderByExecutedAtDesc())
                .thenReturn(Optional.of(etlResult));

        // When
        ResponseEntity<?> response = etlController.getPopularVideos();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        EtlPopularVideoResponse body = (EtlPopularVideoResponse) response.getBody();
        assertNotNull(body);
        assertEquals(2, body.getVideos().size());
    }

    @Test
    void testRunPipeline_Returns200() {
        // Given & When
        ResponseEntity<String> response = etlController.runPipeline();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ETL pipeline uspesno pokrenut.", response.getBody());
        assertTrue(pipelineWasRun, "runEtlPipeline() je trebao biti pozvan");
    }

    @Test
    void testGetPopularVideos_ChecksLatestResult() {
        // Given
        when(etlPipelineResultRepository.findTopByOrderByExecutedAtDesc())
                .thenReturn(Optional.empty());

        // When
        etlController.getPopularVideos();

        // Then - verifikuje da se poziva findTopByOrderByExecutedAtDesc
        verify(etlPipelineResultRepository, times(1)).findTopByOrderByExecutedAtDesc();
    }
}
