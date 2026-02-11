package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EtlPopularVideoResponse {

    private LocalDateTime executedAt;
    private List<EtlVideoEntry> videos;

    public EtlPopularVideoResponse() {}

    public EtlPopularVideoResponse(LocalDateTime executedAt, List<EtlVideoEntry> videos) {
        this.executedAt = executedAt;
        this.videos = videos;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public List<EtlVideoEntry> getVideos() {
        return videos;
    }

    public void setVideos(List<EtlVideoEntry> videos) {
        this.videos = videos;
    }

    public static class EtlVideoEntry {
        private Long videoId;
        private String title;
        private String username;
        private Double popularityScore;
        private Long viewCount;

        public EtlVideoEntry() {}

        public EtlVideoEntry(Long videoId, String title, String username,
                             Double popularityScore, Long viewCount) {
            this.videoId = videoId;
            this.title = title;
            this.username = username;
            this.popularityScore = popularityScore;
            this.viewCount = viewCount;
        }

        public Long getVideoId() {
            return videoId;
        }

        public void setVideoId(Long videoId) {
            this.videoId = videoId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Double getPopularityScore() {
            return popularityScore;
        }

        public void setPopularityScore(Double popularityScore) {
            this.popularityScore = popularityScore;
        }

        public Long getViewCount() {
            return viewCount;
        }

        public void setViewCount(Long viewCount) {
            this.viewCount = viewCount;
        }
    }
}
