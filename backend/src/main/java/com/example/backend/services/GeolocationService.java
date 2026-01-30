package com.example.backend.services;

import com.example.backend.dto.UserLocationRequest;
import com.example.backend.dto.UserLocationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Servis za određivanje lokacije korisnika.
 * 
 * [S2] Zatražiti lokaciju od korisnika, u slučaju da odbije, 
 * koristiti aproksimaciju lokacije sa koje je upućen zahtev.
 * 
 * Strategija:
 * 1. Ako korisnik dozvoli geolokaciju u browser-u -> koristi precise koordinate
 * 2. Ako odbije -> koristi IP geolokaciju kao fallback (manje precizno)
 */
@Service
public class GeolocationService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Besplatan IP geolocation API (ne zahteva API key)
    private static final String IP_API_URL = "http://ip-api.com/json/%s?fields=status,message,country,city,lat,lon";
    
    public GeolocationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Određuje lokaciju korisnika na osnovu request-a.
     * Ako je korisnik dozvolio browser geolokaciju, koristi te koordinate.
     * Ako nije, koristi IP geolokaciju kao fallback.
     */
    public UserLocationResponse resolveUserLocation(UserLocationRequest locationRequest, String clientIp) {
        // Ako korisnik ima validne koordinate iz browser-a
        if (locationRequest != null && locationRequest.isLocationGranted() 
                && locationRequest.hasValidCoordinates()) {
            return UserLocationResponse.fromBrowser(
                locationRequest.getLatitude(), 
                locationRequest.getLongitude()
            );
        }
        
        // Fallback na IP geolokaciju
        return getLocationFromIp(clientIp);
    }
    
    /**
     * Dobija lokaciju na osnovu IP adrese.
     * Koristi besplatan ip-api.com servis.
     */
    public UserLocationResponse getLocationFromIp(String ipAddress) {
        try {
            // Za localhost/development, koristi public IP ili default
            if (isLocalAddress(ipAddress)) {
                // U development modu vraćamo default lokaciju (Beograd)
                return UserLocationResponse.fromIpGeolocation(
                    44.8125, 20.4612, "Belgrade", "Serbia"
                );
            }
            
            String url = String.format(IP_API_URL, ipAddress);
            String response = restTemplate.getForObject(url, String.class);
            
            JsonNode json = objectMapper.readTree(response);
            
            if ("success".equals(json.get("status").asText())) {
                Double lat = json.get("lat").asDouble();
                Double lon = json.get("lon").asDouble();
                String city = json.has("city") ? json.get("city").asText() : null;
                String country = json.has("country") ? json.get("country").asText() : null;
                
                return UserLocationResponse.fromIpGeolocation(lat, lon, city, country);
            } else {
                System.err.println("IP Geolocation failed: " + json.get("message").asText());
                return UserLocationResponse.unknown();
            }
            
        } catch (Exception e) {
            System.err.println("Error getting IP geolocation: " + e.getMessage());
            return UserLocationResponse.unknown();
        }
    }
    
    /**
     * Proverava da li je IP adresa lokalna (localhost, private network).
     */
    private boolean isLocalAddress(String ipAddress) {
        if (ipAddress == null) return true;
        
        return ipAddress.equals("127.0.0.1") 
            || ipAddress.equals("0:0:0:0:0:0:0:1")
            || ipAddress.equals("::1")
            || ipAddress.startsWith("192.168.")
            || ipAddress.startsWith("10.")
            || ipAddress.startsWith("172.16.")
            || ipAddress.startsWith("172.17.")
            || ipAddress.startsWith("172.18.")
            || ipAddress.startsWith("172.19.")
            || ipAddress.startsWith("172.2")
            || ipAddress.startsWith("172.30.")
            || ipAddress.startsWith("172.31.");
    }
    
    /**
     * Računa udaljenost između dve tačke u kilometrima (Haversine formula).
     * Korisno za pretragu videa u blizini.
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Radius Zemlje u km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
