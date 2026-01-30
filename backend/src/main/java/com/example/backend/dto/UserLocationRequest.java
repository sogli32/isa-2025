package com.example.backend.dto;

/**
 * DTO za primanje lokacije korisnika sa frontenda.
 * Ako korisnik dozvoli geolokaciju, frontend šalje lat/lng.
 * Ako odbije, backend koristi IP geolokaciju kao fallback.
 */
public class UserLocationRequest {
    
    private Double latitude;
    private Double longitude;
    private boolean locationGranted; // da li je korisnik dozvolio geolokaciju
    
    public UserLocationRequest() {}
    
    public UserLocationRequest(Double latitude, Double longitude, boolean locationGranted) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationGranted = locationGranted;
    }
    
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
    
    public boolean isLocationGranted() {
        return locationGranted;
    }
    
    public void setLocationGranted(boolean locationGranted) {
        this.locationGranted = locationGranted;
    }
    
    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null 
            && latitude >= -90 && latitude <= 90 
            && longitude >= -180 && longitude <= 180;
    }
}
