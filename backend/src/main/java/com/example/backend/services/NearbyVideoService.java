package com.example.backend.services;

import com.example.backend.config.GeolocationConfig;
import com.example.backend.model.Video;
import com.example.backend.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NearbyVideoService {

    private final VideoRepository videoRepository;
    private final GeolocationConfig geolocationConfig;

    public NearbyVideoService(VideoRepository videoRepository,
                              GeolocationConfig geolocationConfig) {
        this.videoRepository = videoRepository;
        this.geolocationConfig = geolocationConfig;
    }

    public List<Video> findVideosNearby(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null");
        }

        // Ako je radiusKm null, stavi default npr. 10km
        double validatedRadiusKm = (radiusKm != null) ? geolocationConfig.validateRadius(radiusKm) : 10.0;

        // Pretvaranje km u metre: 10km -> 10000m
        double radiusMeters = geolocationConfig.kmToMeters(validatedRadiusKm);

        return videoRepository.findVideosNearby(latitude, longitude, radiusMeters);
    }

    public List<Video> findPopularVideosNearby(Double latitude, Double longitude,
                                               Double radiusKm, Integer limit) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null");
        }

        double validatedRadiusKm = geolocationConfig.validateRadius(radiusKm);
        double radiusMeters = geolocationConfig.kmToMeters(validatedRadiusKm);
        int validatedLimit = (limit != null && limit > 0) ? limit : 20;

        return videoRepository.findPopularVideosNearby(
                latitude, longitude, radiusMeters, validatedLimit
        );
    }
}