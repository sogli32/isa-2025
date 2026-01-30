package com.example.backend.dto;

public class CreateVideoRequest {

    private String title;
    private String description;
    private String tags;
    private String location; // tekstualni opis

    // NOVO: Koordinate
    private Double latitude;
    private Double longitude;

    public CreateVideoRequest() {}

    public CreateVideoRequest(String title, String description, String tags, String location) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.location = location;
    }

    // NOVI konstruktor sa koordinatama
    public CreateVideoRequest(String title, String description, String tags,
                              String location, Double latitude, Double longitude) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
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

    // NOVI getteri/setteri
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

    // Helper metoda za validaciju
    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}