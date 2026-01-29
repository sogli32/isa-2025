package com.example.backend.services;

import com.example.backend.model.Video;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PopularityCalculationService {

    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final CommentRepository commentRepository;
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 5.0;
    private static final double COMMENT_WEIGHT = 10.0;

    public PopularityCalculationService(VideoRepository videoRepository,
                                        VideoLikeRepository videoLikeRepository,
                                        CommentRepository commentRepository) {
        this.videoRepository = videoRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.commentRepository = commentRepository;
    }

    public double calculatePopularityScore(Video video) {
        long viewCount = video.getViewCount();
        long likeCount = videoLikeRepository.countByVideo(video);
        long commentCount = commentRepository.countByVideo(video);
        double engagementScore = (viewCount * VIEW_WEIGHT) +
                (likeCount * LIKE_WEIGHT) +
                (commentCount * COMMENT_WEIGHT);

        //  noviji videi dobijaju boost
        double timeDecay = calculateTimeDecay(video.getCreatedAt());

        return engagementScore * timeDecay;
    }

    private double calculateTimeDecay(LocalDateTime createdAt) {
        long hoursOld = Duration.between(createdAt, LocalDateTime.now()).toHours();
        double daysOld = hoursOld / 24.0;

        // Eksponencijalni decay: 2^(-daysOld / halfLife)
        double halfLifeDays = 7.0;
        return Math.pow(2, -daysOld / halfLifeDays);
    }

    @Scheduled(fixedRate = 900000) // 15 minuta
    @Transactional
    public void updateAllPopularityScores() {
        List<Video> videos = videoRepository.findAll();

        for (Video video : videos) {
            double score = calculatePopularityScore(video);
            video.setPopularityScore(score);
        }

        videoRepository.saveAll(videos);
        System.out.println("Updated popularity scores for " + videos.size() + " videos");
    }

    @Transactional
    public void updateVideoPopularityScore(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        double score = calculatePopularityScore(video);
        video.setPopularityScore(score);
        videoRepository.save(video);
    }
}