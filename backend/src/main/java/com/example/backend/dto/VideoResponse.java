package com.example.backend.dto;

import com.example.backend.model.Video;
import java.time.LocalDateTime;

public class VideoResponse {

    private Long id;
    private String title;
    private String description;
    private String tags;
    private String location;
    private LocalDateTime createdAt;
    private Long viewCount;
    private String username;
    private Long userId;
    private Long likeCount;
    private Double popularityScore;

    // NOVO: Koordinate
    private Double latitude;
    private Double longitude;

    // Zakazano prikazivanje
    private LocalDateTime scheduledAt;
    private boolean available;
    private Long streamOffsetSeconds;

    public VideoResponse() {}

    public VideoResponse(Video video, Long likeCount) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.tags = video.getTags();
        this.location = video.getLocation();
        this.createdAt = video.getCreatedAt();
        this.viewCount = video.getViewCount();
        this.username = video.getUser().getUsername();
        this.userId = video.getUser().getId();
        this.likeCount = likeCount;
        this.popularityScore = video.getPopularityScore();

        // NOVO: Popuni koordinate
        this.latitude = video.getLatitude();
        this.longitude = video.getLongitude();

        // Zakazano prikazivanje
        this.scheduledAt = video.getScheduledAt();
        this.available = video.isAvailable();

        if (video.isScheduled() && video.isAvailable()) {
            long offset = java.time.Duration.between(video.getScheduledAt(), LocalDateTime.now()).getSeconds();
            this.streamOffsetSeconds = Math.max(0, offset);
        } else {
            this.streamOffsetSeconds = null;
        }
    }

    // Svi postojeći getteri/setteri...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    // NOVI getteri/setteri za koordinate
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Long getStreamOffsetSeconds() {
        return streamOffsetSeconds;
    }

    public void setStreamOffsetSeconds(Long streamOffsetSeconds) {
        this.streamOffsetSeconds = streamOffsetSeconds;
    }
}