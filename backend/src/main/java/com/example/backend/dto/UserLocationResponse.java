package com.example.backend.dto;

/**
 * DTO za vraćanje lokacije korisnika.
 * Sadrži koordinate i informaciju o izvoru lokacije.
 */
public class UserLocationResponse {
    
    private Double latitude;
    private Double longitude;
    private String city;
    private String country;
    private String source; // "browser" ili "ip_geolocation"
    private boolean approximate; // true ako je IP geolokacija (manje precizna)
    
    public UserLocationResponse() {}
    
    public UserLocationResponse(Double latitude, Double longitude, String city, 
                                 String country, String source, boolean approximate) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.country = country;
        this.source = source;
        this.approximate = approximate;
    }
    
    // Static factory methods
    public static UserLocationResponse fromBrowser(Double latitude, Double longitude) {
        return new UserLocationResponse(latitude, longitude, null, null, "browser", false);
    }
    
    public static UserLocationResponse fromIpGeolocation(Double latitude, Double longitude, 
                                                          String city, String country) {
        return new UserLocationResponse(latitude, longitude, city, country, "ip_geolocation", true);
    }
    
    public static UserLocationResponse unknown() {
        return new UserLocationResponse(null, null, null, null, "unknown", true);
    }
    
    // Getters and Setters
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
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public boolean isApproximate() {
        return approximate;
    }
    
    public void setApproximate(boolean approximate) {
        this.approximate = approximate;
    }
    
    public boolean hasValidLocation() {
        return latitude != null && longitude != null;
    }
}
