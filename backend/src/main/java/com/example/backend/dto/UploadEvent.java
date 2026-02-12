package com.example.backend.dto;

import java.util.List;

public class UploadEvent {
    private Long videoId;
    private String title;
    private String username;
    private Long fileSize;
    private String uploadTime;
    private String location;
    private List<String> tags;

    public UploadEvent() {
    }

    public UploadEvent(Long videoId, String title, String username, Long fileSize, 
                      String uploadTime, String location, List<String> tags) {
        this.videoId = videoId;
        this.title = title;
        this.username = username;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
        this.location = location;
        this.tags = tags;
    }

    // Getters and Setters
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "UploadEvent{" +
                "videoId=" + videoId +
                ", title='" + title + '\'' +
                ", username='" + username + '\'' +
                ", fileSize=" + fileSize +
                ", uploadTime='" + uploadTime + '\'' +
                ", location='" + location + '\'' +
                ", tags=" + tags +
                '}';
    }
}
