package com.example.backend.controller;

import com.example.backend.dto.UserLocationRequest;
import com.example.backend.dto.UserLocationResponse;
import com.example.backend.services.GeolocationService;
import com.example.backend.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kontroler za geolokaciju korisnika.
 * 
 * [S2] Zatražiti lokaciju od korisnika, u slučaju da odbije, 
 * koristiti aproksimaciju lokacije sa koje je upućen zahtev.
 */
@RestController
@RequestMapping("/api/geolocation")
@CrossOrigin(origins = "http://localhost:4200")
public class GeolocationController {
    
    private final GeolocationService geolocationService;
    
    public GeolocationController(GeolocationService geolocationService) {
        this.geolocationService = geolocationService;
    }
    
    /**
     * Endpoint za dobijanje/resolving lokacije korisnika.
     * 
     * Frontend treba da:
     * 1. Zatraži dozvolu za geolokaciju od korisnika
     * 2. Ako korisnik dozvoli - pošalje lat/lng u body
     * 3. Ako odbije - pošalje locationGranted: false
     * 
     * Backend će:
     * - Ako ima koordinate iz browser-a -> vrati ih
     * - Ako nema -> koristi IP geolokaciju kao fallback
     */
    @PostMapping("/resolve")
    public ResponseEntity<UserLocationResponse> resolveLocation(
            @RequestBody(required = false) UserLocationRequest locationRequest,
            HttpServletRequest request
    ) {
        String clientIp = IpUtil.getClientIp(request);
        
        UserLocationResponse location = geolocationService.resolveUserLocation(
            locationRequest, clientIp
        );
        
        return ResponseEntity.ok(location);
    }
    
    /**
     * Endpoint za dobijanje lokacije samo na osnovu IP adrese.
     * Koristan kada frontend ne može da dobije browser geolokaciju.
     */
    @GetMapping("/ip-location")
    public ResponseEntity<UserLocationResponse> getIpLocation(HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        
        UserLocationResponse location = geolocationService.getLocationFromIp(clientIp);
        
        return ResponseEntity.ok(location);
    }
}
