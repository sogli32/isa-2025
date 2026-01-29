package com.example.backend;

import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.services.PopularityCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularityCalculationServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PopularityCalculationService popularityCalculationService;

    private Video testVideo;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@test.com");

        testVideo = new Video();
        testVideo.setId(1L);
        testVideo.setTitle("Test Video");
        testVideo.setViewCount(100L);
        testVideo.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testCalculatePopularityScore_WithNoEngagement() {
        // Given
        when(videoLikeRepository.countByVideo(testVideo)).thenReturn(0L);
        when(commentRepository.countByVideo(testVideo)).thenReturn(0L);
        testVideo.setViewCount(0L);

        // When
        double score = popularityCalculationService.calculatePopularityScore(testVideo);

        // Then
        assertEquals(0.0, score, 0.01);
    }

    @Test
    void testCalculatePopularityScore_WithViewsOnly() {
        // Given
        when(videoLikeRepository.countByVideo(testVideo)).thenReturn(0L);
        when(commentRepository.countByVideo(testVideo)).thenReturn(0L);
        testVideo.setViewCount(100L);

        // When
        double score = popularityCalculationService.calculatePopularityScore(testVideo);

        // Then
        // 100 views * 1.0 weight * time_decay (oko 0.9 za 1 dan star video)
        assertTrue(score > 80 && score < 100);
    }

    @Test
    void testCalculatePopularityScore_WithLikes() {
        // Given
        when(videoLikeRepository.countByVideo(testVideo)).thenReturn(10L);
        when(commentRepository.countByVideo(testVideo)).thenReturn(0L);
        testVideo.setViewCount(0L);

        // When
        double score = popularityCalculationService.calculatePopularityScore(testVideo);

        // Then
        // 10 likes * 5.0 weight = 50 * time_decay
        assertTrue(score > 40 && score < 50);
    }

    @Test
    void testCalculatePopularityScore_WithComments() {
        // Given
        when(videoLikeRepository.countByVideo(testVideo)).thenReturn(0L);
        when(commentRepository.countByVideo(testVideo)).thenReturn(5L);
        testVideo.setViewCount(0L);

        // When
        double score = popularityCalculationService.calculatePopularityScore(testVideo);

        // Then
        // 5 comments * 10.0 weight = 50 * time_decay
        assertTrue(score > 40 && score < 50);
    }

    @Test
    void testCalculatePopularityScore_CombinedEngagement() {
        // Given
        when(videoLikeRepository.countByVideo(testVideo)).thenReturn(20L);
        when(commentRepository.countByVideo(testVideo)).thenReturn(5L);
        testVideo.setViewCount(100L);

        // When
        double score = popularityCalculationService.calculatePopularityScore(testVideo);

        // Then
        // (100*1 + 20*5 + 5*10) * time_decay = 250 * ~0.9 = ~225
        assertTrue(score > 200 && score < 250);
    }

    @Test
    void testCalculatePopularityScore_NewerVideoHasHigherScore() {
        // Given - Video star 1 dan
        Video newVideo = new Video();
        newVideo.setViewCount(100L);
        newVideo.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(videoLikeRepository.countByVideo(newVideo)).thenReturn(10L);
        when(commentRepository.countByVideo(newVideo)).thenReturn(0L);

        // Video star 14 dana
        Video oldVideo = new Video();
        oldVideo.setViewCount(100L);
        oldVideo.setCreatedAt(LocalDateTime.now().minusDays(14));

        when(videoLikeRepository.countByVideo(oldVideo)).thenReturn(10L);
        when(commentRepository.countByVideo(oldVideo)).thenReturn(0L);

        // When
        double newScore = popularityCalculationService.calculatePopularityScore(newVideo);
        double oldScore = popularityCalculationService.calculatePopularityScore(oldVideo);

        // Then
        assertTrue(newScore > oldScore,
                "Noviji video bi trebao imati veći score: " + newScore + " > " + oldScore);
    }

    @Test
    void testUpdateVideoPopularityScore() {
        // Given
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoLikeRepository.countByVideo(testVideo)).thenReturn(10L);
        when(commentRepository.countByVideo(testVideo)).thenReturn(5L);
        when(videoRepository.save(any(Video.class))).thenReturn(testVideo);

        // When
        popularityCalculationService.updateVideoPopularityScore(1L);

        // Then
        verify(videoRepository, times(1)).findById(1L);
        verify(videoRepository, times(1)).save(testVideo);
        assertTrue(testVideo.getPopularityScore() > 0);
    }

    @Test
    void testUpdateVideoPopularityScore_VideoNotFound() {
        // Given
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            popularityCalculationService.updateVideoPopularityScore(999L);
        });
    }
}