package com.example.backend;

import com.example.backend.dto.VideoResponse;
import com.example.backend.model.Comment;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.model.VideoLike;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.services.PopularityCalculationService;
import com.example.backend.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VideoServiceIntegrationTest {

    @Autowired
    private VideoService videoService;

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
        // Kreiraj test korisnika SA SVIM OBAVEZNIM POLJIMA
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address 123");
        testUser.setRole("USER");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testTrendingVideos_OrderedByPopularityScore() {
        // Given - Kreiraj 3 videa sa različitim engagement-om

        // Video 1: Malo engagement-a
        Video video1 = createVideo("Video 1", 10L, LocalDateTime.now().minusDays(1));
        addLikes(video1, 2);
        addComments(video1, 0);

        // Video 2: Puno engagement-a
        Video video2 = createVideo("Video 2", 100L, LocalDateTime.now().minusHours(2));
        addLikes(video2, 30);
        addComments(video2, 5);

        // Video 3: Srednji engagement
        Video video3 = createVideo("Video 3", 50L, LocalDateTime.now().minusDays(2));
        addLikes(video3, 10);
        addComments(video3, 2);

        // When - Izračunaj skorove
        popularityCalculationService.updateAllPopularityScores();

        // Dohvati trending videe
        List<VideoResponse> trending = videoService.getTrendingVideos(10);

        // Then
        assertEquals(3, trending.size());

        // Video 2 bi trebao biti #1 (najviše engagement + najnoviji)
        assertEquals("Video 2", trending.get(0).getTitle());

        // Video 3 bi trebao biti #2
        assertEquals("Video 3", trending.get(1).getTitle());

        // Video 1 bi trebao biti #3
        assertEquals("Video 1", trending.get(2).getTitle());
    }

    @Test
    void testPopularityScoreUpdatesAfterLike() {
        // Given
        Video video = createVideo("Test Video", 0L, LocalDateTime.now());
        popularityCalculationService.updateVideoPopularityScore(video.getId());
        double initialScore = videoRepository.findById(video.getId()).get().getPopularityScore();

        // When - Dodaj like
        videoService.toggleLike(video.getId(), testUser.getEmail());

        // Then
        Video updatedVideo = videoRepository.findById(video.getId()).get();
        assertTrue(updatedVideo.getPopularityScore() > initialScore,
                "Score bi trebao porasti nakon like-a: " + initialScore + " -> " + updatedVideo.getPopularityScore());
    }

    @Test
    void testPopularityScoreUpdatesAfterView() {
        // Given
        Video video = createVideo("Test Video", 0L, LocalDateTime.now());
        popularityCalculationService.updateVideoPopularityScore(video.getId());
        double initialScore = videoRepository.findById(video.getId()).get().getPopularityScore();

        // When - Dodaj view
        videoService.incrementViewCount(video.getId());

        // Then
        Video updatedVideo = videoRepository.findById(video.getId()).get();
        assertTrue(updatedVideo.getPopularityScore() > initialScore,
                "Score bi trebao porasti nakon view-a: " + initialScore + " -> " + updatedVideo.getPopularityScore());
    }

    @Test
    void testNewerVideoWithSameEngagementHasHigherScore() {
        // Given - Kreiraj 2 videa sa istim engagement-om ali različitom starošću
        Video oldVideo = createVideo("Old Video", 100L, LocalDateTime.now().minusDays(14));
        addLikes(oldVideo, 10);

        Video newVideo = createVideo("New Video", 100L, LocalDateTime.now().minusHours(1));
        addLikes(newVideo, 10);

        // When
        popularityCalculationService.updateAllPopularityScores();

        // Then
        Video oldVideoRefreshed = videoRepository.findById(oldVideo.getId()).get();
        Video newVideoRefreshed = videoRepository.findById(newVideo.getId()).get();

        assertTrue(newVideoRefreshed.getPopularityScore() > oldVideoRefreshed.getPopularityScore(),
                "Noviji video bi trebao imati veći score: " +
                        newVideoRefreshed.getPopularityScore() + " > " + oldVideoRefreshed.getPopularityScore());
    }

    @Test
    void testLikesOutweighFewerComments() {
        // Given
        Video videoWithLikes = createVideo("Video with Likes", 0L, LocalDateTime.now());
        addLikes(videoWithLikes, 10); // 10 likes * 5 = 50

        Video videoWithComments = createVideo("Video with Comments", 0L, LocalDateTime.now());
        addComments(videoWithComments, 3); // 3 comments * 10 = 30

        // When
        popularityCalculationService.updateAllPopularityScores();

        // Then
        Video likesVideo = videoRepository.findById(videoWithLikes.getId()).get();
        Video commentsVideo = videoRepository.findById(videoWithComments.getId()).get();

        assertTrue(likesVideo.getPopularityScore() > commentsVideo.getPopularityScore(),
                "Video sa 10 likes bi trebao imati veći score od videa sa 3 comments");
    }

    // Helper metode
    private Video createVideo(String title, Long viewCount, LocalDateTime createdAt) {
        Video video = new Video(
                title,
                "Description",
                "tags",
                "thumbnail.jpg",
                "video.mp4",
                testUser,
                null
        );
        video.setViewCount(viewCount);
        video.setCreatedAt(createdAt);
        return videoRepository.save(video);
    }

    private void addLikes(Video video, int count) {
        for (int i = 0; i < count; i++) {
            User likeUser = new User();
            likeUser.setEmail(UUID.randomUUID() + "@test.com");
            likeUser.setUsername("user_" + UUID.randomUUID());
            likeUser.setPassword("pass");
            likeUser.setFirstName("User");
            likeUser.setLastName("Test");
            likeUser.setAddress("Address");
            likeUser.setRole("USER");
            likeUser.setEnabled(true);

            likeUser = userRepository.save(likeUser);
            videoLikeRepository.save(new VideoLike(video, likeUser));
        }
    }


    private void addComments(Video video, int count) {
        for (int i = 0; i < count; i++) {
            Comment comment = new Comment("Comment " + i, testUser, video);
            commentRepository.save(comment);
        }
    }
}