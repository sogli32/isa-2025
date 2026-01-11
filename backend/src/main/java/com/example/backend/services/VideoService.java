package com.example.backend.services;

import com.example.backend.dto.CreateVideoRequest;
import com.example.backend.dto.VideoResponse;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ThumbnailCacheService thumbnailCacheService;

    public VideoService(VideoRepository videoRepository,
                        UserRepository userRepository,
                        FileStorageService fileStorageService,
                        ThumbnailCacheService thumbnailCacheService) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.thumbnailCacheService = thumbnailCacheService;
    }


    @Transactional(rollbackOn = Exception.class)
    public VideoResponse createVideo(
            CreateVideoRequest request,
            MultipartFile videoFile,
            MultipartFile thumbnailFile,
            String userEmail
    ) throws IOException {

        // Validacija
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }

        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            throw new IllegalArgumentException("Thumbnail image is required");
        }

        // Pronalaženje korisnika
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String videoPath = null;
        String thumbnailPath = null;

        try {
            // Upload videa 
            videoPath = fileStorageService.uploadVideo(videoFile);

            // Upload thumbnail
            thumbnailPath = fileStorageService.uploadThumbnail(thumbnailFile);

            // Kreiranje video objekta
            Video video = new Video(
                    request.getTitle(),
                    request.getDescription(),
                    request.getTags(),
                    thumbnailPath,
                    videoPath,
                    user,
                    request.getLocation()
            );

            // Čuvanje u bazu
            video = videoRepository.save(video);

            return new VideoResponse(video);

        } catch (IOException e) {
           
            if (videoPath != null) {
                fileStorageService.deleteFile(videoPath, true);
            }
            if (thumbnailPath != null) {
                fileStorageService.deleteFile(thumbnailPath, false);
            }

        
            throw new RuntimeException("Failed to upload files: " + e.getMessage(), e);
        }
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(VideoResponse::new)
                .collect(Collectors.toList());
    }

    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return new VideoResponse(video);
    }

    @Transactional
    public void incrementViewCount(Long videoId) {
        int updatedRows = videoRepository.incrementViewCount(videoId);
        
        if (updatedRows == 0) {
            throw new IllegalArgumentException("Video not found");
        }
    }

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

    @Transactional
    public void deleteVideo(Long videoId, String userEmail) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (!video.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("You don't have permission to delete this video");
        }

        // Brisanje fajlova
        fileStorageService.deleteFile(video.getVideoPath(), true);
        fileStorageService.deleteFile(video.getThumbnailPath(), false);

        // Brisanje iz keša
        thumbnailCacheService.evict(video.getThumbnailPath());

        // Brisanje iz baze
        videoRepository.delete(video);
    }
}
