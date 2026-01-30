package com.example.backend.controller;

import com.example.backend.dto.UserLocationRequest;
import com.example.backend.dto.UserLocationResponse;
import com.example.backend.model.Video;
import com.example.backend.services.GeolocationService;
import com.example.backend.services.NearbyVideoService;  // DODAJ IMPORT
import com.example.backend.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kontroler za geolokaciju korisnika.
 *
 * [S2] Zatražiti lokaciju od korisnika, u slučaju da odbije,
 * koristiti aproksimaciju lokacije sa koje je upućen zahtev.
 */
@RestController
@RequestMapping("/api/geolocation")
@CrossOrigin(origins = "http://localhost:4200")
public class GeolocationController {

    private final GeolocationService geolocationService;
    private final NearbyVideoService nearbyVideoService;  // DODAJ OVO

    // IZMENI KONSTRUKTOR
    public GeolocationController(GeolocationService geolocationService,
                                 NearbyVideoService nearbyVideoService) {
        this.geolocationService = geolocationService;
        this.nearbyVideoService = nearbyVideoService;
    }

    @PostMapping("/resolve")
    public ResponseEntity<UserLocationResponse> resolveLocation(
            @RequestBody(required = false) UserLocationRequest locationRequest,
            HttpServletRequest request
    ) {
        String clientIp = IpUtil.getClientIp(request);

        UserLocationResponse location = geolocationService.resolveUserLocation(
                locationRequest, clientIp
        );

        return ResponseEntity.ok(location);
    }

    @GetMapping("/ip-location")
    public ResponseEntity<UserLocationResponse> getIpLocation(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);

        UserLocationResponse location = geolocationService.getLocationFromIp(clientIp);

        return ResponseEntity.ok(location);
    }

    @PostMapping("/videos/nearby")
    public ResponseEntity<List<Video>> getVideosNearby(
            @RequestBody UserLocationRequest locationRequest,
            @RequestParam(required = false) Double radiusKm,
            HttpServletRequest request
    ) {
        if (locationRequest == null || !locationRequest.hasValidCoordinates()) {
            String clientIp = IpUtil.getClientIp(request);
            UserLocationResponse location = geolocationService.getLocationFromIp(clientIp);

            if (!location.hasValidLocation()) {
                return ResponseEntity.badRequest().build();
            }

            List<Video> videos = nearbyVideoService.findVideosNearby(
                    location.getLatitude(),
                    location.getLongitude(),
                    radiusKm
            );

            return ResponseEntity.ok(videos);
        }

        List<Video> videos = nearbyVideoService.findVideosNearby(
                locationRequest.getLatitude(),
                locationRequest.getLongitude(),
                radiusKm
        );

        return ResponseEntity.ok(videos);
    }

    @PostMapping("/videos/popular-nearby")
    public ResponseEntity<List<Video>> getPopularVideosNearby(
            @RequestBody UserLocationRequest locationRequest,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest request
    ) {
        if (locationRequest == null || !locationRequest.hasValidCoordinates()) {
            String clientIp = IpUtil.getClientIp(request);
            UserLocationResponse location = geolocationService.getLocationFromIp(clientIp);

            if (!location.hasValidLocation()) {
                return ResponseEntity.badRequest().build();
            }

            List<Video> videos = nearbyVideoService.findPopularVideosNearby(
                    location.getLatitude(),
                    location.getLongitude(),
                    radiusKm,
                    limit
            );

            return ResponseEntity.ok(videos);
        }

        List<Video> videos = nearbyVideoService.findPopularVideosNearby(
                locationRequest.getLatitude(),
                locationRequest.getLongitude(),
                radiusKm,
                limit
        );

        return ResponseEntity.ok(videos);
    }
}