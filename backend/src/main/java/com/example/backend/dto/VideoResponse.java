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
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

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
}
