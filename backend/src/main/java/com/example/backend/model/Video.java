package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String tags;

    @Column(nullable = false, length = 500)
    private String thumbnailPath;

    @Column(nullable = false, length = 500)
    private String videoPath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String location; // tekstualni opis (grad, država)

    // NOVI: Koordinate za prostorno indeksiranje
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Double popularityScore = 0.0;

    public Video() {
        this.createdAt = LocalDateTime.now();
    }

    public Video(String title, String description, String tags, String thumbnailPath,
                 String videoPath, User user, String location) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.user = user;
        this.location = location;
        this.createdAt = LocalDateTime.now();
        this.viewCount = 0L;
    }

    // Svi postojeći getteri/setteri...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }
}