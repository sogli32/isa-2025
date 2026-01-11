package com.example.backend.dto;

public class CreateVideoRequest {
    
    private String title;
    private String description;
    private String tags;
    private String location;

    public CreateVideoRequest() {}

    public CreateVideoRequest(String title, String description, String tags, String location) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.location = location;
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
}
