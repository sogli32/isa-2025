package com.example.backend.controller;

import com.example.backend.dto.UserLocationRequest;
import com.example.backend.dto.UserLocationResponse;
import com.example.backend.dto.VideoResponse;
import com.example.backend.model.Video;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.services.GeolocationService;
import com.example.backend.services.NearbyVideoService;
import com.example.backend.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Kontroler za geolokaciju korisnika i pretragu videa u blizini.
 */
@RestController
@RequestMapping("/api/geolocation")
@CrossOrigin(origins = "http://localhost:4200")
public class GeolocationController {

    private final GeolocationService geolocationService;
    private final NearbyVideoService nearbyVideoService;
    private final VideoLikeRepository videoLikeRepository; // Neophodno za mapiranje u VideoResponse

    public GeolocationController(GeolocationService geolocationService,
                                 NearbyVideoService nearbyVideoService,
                                 VideoLikeRepository videoLikeRepository) {
        this.geolocationService = geolocationService;
        this.nearbyVideoService = nearbyVideoService;
        this.videoLikeRepository = videoLikeRepository;
    }

    @PostMapping("/resolve")
    public ResponseEntity<UserLocationResponse> resolveLocation(
            @RequestBody(required = false) UserLocationRequest locationRequest,
            HttpServletRequest request
    ) {
        String clientIp = IpUtil.getClientIp(request);
        UserLocationResponse location = geolocationService.resolveUserLocation(locationRequest, clientIp);
        return ResponseEntity.ok(location);
    }

    @GetMapping("/ip-location")
    public ResponseEntity<UserLocationResponse> getIpLocation(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        UserLocationResponse location = geolocationService.getLocationFromIp(clientIp);
        return ResponseEntity.ok(location);
    }

    @PostMapping("/videos/nearby")
    public ResponseEntity<List<VideoResponse>> getVideosNearby(
            @RequestBody UserLocationRequest locationRequest,
            @RequestParam(required = false) Double radiusKm,
            HttpServletRequest request
    ) {
        UserLocationResponse userLoc = getEffectiveLocation(locationRequest, request);

        if (!userLoc.hasValidLocation()) {
            return ResponseEntity.badRequest().build();
        }

        List<Video> videos = nearbyVideoService.findVideosNearby(
                userLoc.getLatitude(),
                userLoc.getLongitude(),
                radiusKm
        );

        return ResponseEntity.ok(mapToVideoResponse(videos));
    }

    @PostMapping("/videos/popular-nearby")
    public ResponseEntity<List<VideoResponse>> getPopularVideosNearby(
            @RequestBody UserLocationRequest locationRequest,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest request
    ) {
        UserLocationResponse userLoc = getEffectiveLocation(locationRequest, request);

        if (!userLoc.hasValidLocation()) {
            return ResponseEntity.badRequest().build();
        }

        List<Video> videos = nearbyVideoService.findPopularVideosNearby(
                userLoc.getLatitude(),
                userLoc.getLongitude(),
                radiusKm,
                limit
        );

        return ResponseEntity.ok(mapToVideoResponse(videos));
    }

    /**
     * Pomoćna metoda za određivanje lokacije (Browser koordinate ili IP fallback).
     */
    private UserLocationResponse getEffectiveLocation(UserLocationRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.hasValidCoordinates()) {
            return UserLocationResponse.fromBrowser(request.getLatitude(), request.getLongitude());
        }
        String clientIp = IpUtil.getClientIp(httpRequest);
        return geolocationService.getLocationFromIp(clientIp);
    }

    /**
     * Pomoćna metoda koja transformiše model Video u DTO VideoResponse.
     * Ovim se rešava problem sa 'username' i 'likes' na frontendu.
     */
    private List<VideoResponse> mapToVideoResponse(List<Video> videos) {
        return videos.stream()
                .filter(Video::isAvailable)
                .map(v -> {
                    Long likesCount = videoLikeRepository.countByVideo(v);
                    return new VideoResponse(v, likesCount);
                })
                .collect(Collectors.toList());
    }
}