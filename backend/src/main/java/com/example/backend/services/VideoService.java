package com.example.backend.services;

import com.example.backend.dto.CreateVideoRequest;
import com.example.backend.dto.UserLocationResponse;
import com.example.backend.dto.VideoResponse;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.model.VideoLike;
import com.example.backend.model.VideoView;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.repository.VideoViewRepository;
import com.example.backend.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final VideoViewRepository videoViewRepository;
    private final FileStorageService fileStorageService;
    private final ThumbnailCacheService thumbnailCacheService;
    private final PopularityCalculationService popularityCalculationService;
    private final GeolocationService geolocationService;
    private final VideoUploadPublisher videoUploadPublisher;

    public VideoService(VideoRepository videoRepository,
                        UserRepository userRepository,
                        VideoLikeRepository videoLikeRepository,
                        VideoViewRepository videoViewRepository,
                        FileStorageService fileStorageService,
                        ThumbnailCacheService thumbnailCacheService,
                        PopularityCalculationService popularityCalculationService,
                        GeolocationService geolocationService,
                        VideoUploadPublisher videoUploadPublisher) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.videoViewRepository = videoViewRepository;
        this.fileStorageService = fileStorageService;
        this.thumbnailCacheService = thumbnailCacheService;
        this.popularityCalculationService = popularityCalculationService;
        this.geolocationService = geolocationService;
        this.videoUploadPublisher = videoUploadPublisher;
    }

    // ================= CREATE VIDEO =================
    @Transactional(rollbackOn = Exception.class)
    public VideoResponse createVideo(
            CreateVideoRequest request,
            MultipartFile videoFile,
            MultipartFile thumbnailFile,
            String userEmail,
            HttpServletRequest httpRequest) throws IOException {

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            throw new IllegalArgumentException("Thumbnail image is required");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String videoPath = null;
        String thumbnailPath = null;

        try {
            videoPath = fileStorageService.uploadVideo(videoFile);
            thumbnailPath = fileStorageService.uploadThumbnail(thumbnailFile);

            Video video = new Video(
                    request.getTitle(),
                    request.getDescription(),
                    request.getTags(),
                    thumbnailPath,
                    videoPath,
                    user,
                    request.getLocation()
            );

            // Postavi zakazano vreme ako postoji
            if (request.getScheduledAt() != null) {
                video.setScheduledAt(request.getScheduledAt());
            }

            // Postavi koordinate
            if (request.hasValidCoordinates()) {
                video.setLatitude(request.getLatitude());
                video.setLongitude(request.getLongitude());
            } else {
                String clientIp = IpUtil.getClientIp(httpRequest);
                UserLocationResponse ipLocation = geolocationService.getLocationFromIp(clientIp);

                if (ipLocation.hasValidLocation()) {
                    video.setLatitude(ipLocation.getLatitude());
                    video.setLongitude(ipLocation.getLongitude());

                    if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
                        String locationText = ipLocation.getCity() + ", " + ipLocation.getCountry();
                        video.setLocation(locationText);
                    }
                }
            }

            // Sačuvaj u bazu
            video = videoRepository.save(video);

            // Publish na RabbitMQ (JSON + Protobuf)
            long fileSize = videoFile.getSize();
            videoUploadPublisher.publishJsonEvent(video, fileSize);
            videoUploadPublisher.publishProtobufEvent(video, fileSize);

            return new VideoResponse(video, 0L);

        } catch (IOException e) {
            if (videoPath != null) fileStorageService.deleteFile(videoPath, true);
            if (thumbnailPath != null) fileStorageService.deleteFile(thumbnailPath, false);
            throw new RuntimeException("Failed to upload files: " + e.getMessage(), e);
        }
    }

    // ================= GET VIDEOS =================
    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(Video::isAvailable)
                .map(video -> {
                    Long likes = videoLikeRepository.countByVideo(video);
                    return new VideoResponse(video, likes);
                })
                .collect(Collectors.toList());
    }

    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        Long likes = videoLikeRepository.countByVideo(video);
        return new VideoResponse(video, likes);
    }

    // ================= SCHEDULED VIDEO HELPERS =================
    public boolean isVideoAvailable(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return video.isAvailable();
    }

    public Map<String, Object> getStreamInfo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Map<String, Object> info = new HashMap<>();
        info.put("videoId", videoId);
        info.put("scheduled", video.isScheduled());
        info.put("available", video.isAvailable());
        info.put("scheduledAt", video.getScheduledAt());

        if (video.isScheduled() && video.isAvailable()) {
            long offsetSeconds = Duration.between(video.getScheduledAt(), LocalDateTime.now()).getSeconds();
            info.put("streamOffsetSeconds", Math.max(0, offsetSeconds));
        } else if (video.isScheduled() && !video.isAvailable()) {
            long secondsUntilAvailable = Duration.between(LocalDateTime.now(), video.getScheduledAt()).getSeconds();
            info.put("secondsUntilAvailable", Math.max(0, secondsUntilAvailable));
        }

        return info;
    }

    // ================= VIEW COUNT =================
    @Transactional
    public void incrementViewCount(Long videoId) {
        int updatedRows = videoRepository.incrementViewCount(videoId);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("Video not found");
        }

        // Loguj pojedinacni pregled za ETL pipeline
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        videoViewRepository.save(new VideoView(video));

        popularityCalculationService.updateVideoPopularityScore(videoId);
    }

    // ================= FILES =================
    public byte[] getVideoFile(Long videoId) throws IOException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return fileStorageService.loadFile(video.getVideoPath(), true);
    }

    public byte[] getThumbnail(Long videoId) throws IOException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return thumbnailCacheService.getThumbnail(video.getThumbnailPath());
    }

    // ================= LIKE / UNLIKE =================
    public Long getLikesCount(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return videoLikeRepository.countByVideo(video);
    }

    public boolean toggleLike(Long videoId, String userEmail) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<VideoLike> existingLike = videoLikeRepository.findByVideoAndUser(video, user);

        if (existingLike.isPresent()) {
            videoLikeRepository.delete(existingLike.get());
            popularityCalculationService.updateVideoPopularityScore(videoId);
            return false;
        } else {
            VideoLike like = new VideoLike(video, user);
            videoLikeRepository.save(like);
            popularityCalculationService.updateVideoPopularityScore(videoId);
            return true;
        }
    }

    // ================= DELETE VIDEO =================
    @Transactional
    public void deleteVideo(Long videoId, String userEmail) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (!video.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You don't have permission to delete this video");
        }

        fileStorageService.deleteFile(video.getVideoPath(), true);
        fileStorageService.deleteFile(video.getThumbnailPath(), false);
        thumbnailCacheService.evict(video.getThumbnailPath());

        videoLikeRepository.deleteAllByVideo(video);

        videoRepository.delete(video);
    }

    public List<VideoResponse> getTrendingVideos(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return videoRepository.findTrendingVideos(pageable)
                .stream()
                .filter(Video::isAvailable)
                .map(video -> {
                    Long likes = videoLikeRepository.countByVideo(video);
                    return new VideoResponse(video, likes);
                })
                .collect(Collectors.toList());
    }
}
