package com.example.backend.controller;

import com.example.backend.dto.CreateVideoRequest;
import com.example.backend.dto.VideoResponse;
import com.example.backend.security.JwtUtil;
import com.example.backend.services.GeolocationService;
import com.example.backend.services.VideoService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
//@CrossOrigin(origins = "http://localhost:4200")
public class VideoController {

    private final VideoService videoService;
    private final JwtUtil jwtUtil;
    private final GeolocationService geolocationService;

    public VideoController(VideoService videoService, JwtUtil jwtUtil,GeolocationService geolocationService) {
        this.videoService = videoService;
        this.jwtUtil = jwtUtil;
        this.geolocationService = geolocationService;
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("tags") String tags,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,

            // NOVO: Koordinate (opciono - fallback na IP geolokaciju)
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,

            // Zakazano prikazivanje (opciono - ISO format: 2025-01-15T08:00:00)
            @RequestParam(value = "scheduledAt", required = false) String scheduledAtStr,

            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest  // DODAJ OVO za IP geolokaciju fallback
    ) {
        try {
            String userEmail = extractEmailFromToken(authHeader);

            LocalDateTime scheduledAt = null;
            if (scheduledAtStr != null && !scheduledAtStr.trim().isEmpty()) {
                try {
                    scheduledAt = LocalDateTime.parse(scheduledAtStr);
                    if (scheduledAt.isBefore(LocalDateTime.now())) {
                        return ResponseEntity.badRequest().body("Zakazano vreme mora biti u budućnosti");
                    }
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest().body("Neispravan format datuma. Koristite ISO format: 2025-01-15T08:00:00");
                }
            }

            CreateVideoRequest request = new CreateVideoRequest(
                    title, description, tags, location, latitude, longitude, scheduledAt
            );

            VideoResponse video = videoService.createVideo(
                    request, videoFile, thumbnailFile, userEmail, httpRequest
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(video);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload video: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }


    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        List<VideoResponse> videos = videoService.getAllVideos();
        return ResponseEntity.ok(videos);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoById(@PathVariable Long id) {
        try {
            VideoResponse video = videoService.getVideoById(id);
            return ResponseEntity.ok(video);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

 
    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long id) {
        try {
            videoService.incrementViewCount(id);
            return ResponseEntity.ok("View count incremented");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to increment view count: " + e.getMessage());
        }
    }



    @GetMapping("/{id}/stream")
    public ResponseEntity<?> streamVideo(@PathVariable Long id) {
        try {
            // Proveri da li je video dostupan (zakazani videi)
            if (!videoService.isVideoAvailable(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Video još nije dostupan. Zakazan je za kasnije prikazivanje.");
            }

            byte[] videoData = videoService.getVideoFile(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            headers.setContentLength(videoData.length);
            headers.set("Accept-Ranges", "bytes");

            return new ResponseEntity<>(videoData, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to load video: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/stream-info")
    public ResponseEntity<?> getStreamInfo(@PathVariable Long id) {
        try {
            Map<String, Object> info = videoService.getStreamInfo(id);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<?> getThumbnail(@PathVariable Long id) {
        try {
            byte[] thumbnailData = videoService.getThumbnail(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(thumbnailData.length);
            
            return new ResponseEntity<>(thumbnailData, headers, HttpStatus.OK);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to load thumbnail: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String userEmail = extractEmailFromToken(authHeader);
            videoService.deleteVideo(id, userEmail);
            return ResponseEntity.ok("Video deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete video: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeVideo(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String userEmail = extractEmailFromToken(authHeader);
            boolean liked = videoService.toggleLike(id, userEmail);
            Long likesCount = videoService.getLikesCount(id);

            // Korišćenje HashMap da bude kompatibilno
            Map<String, Object> response = new HashMap<>();
            response.put("liked", liked);
            response.put("likeCount", likesCount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<List<VideoResponse>> getTrendingVideos(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(videoService.getTrendingVideos(limit));
    }

    private String extractEmailFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtUtil.validateToken(token).getBody();
        return claims.getSubject();
    }
}
