package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.geolocation")
public class GeolocationConfig {

    private double defaultRadiusKm = 50.0;
    private double maxRadiusKm = 500.0;
    private double minRadiusKm = 1.0;

    public double getDefaultRadiusKm() {
        return defaultRadiusKm;
    }

    public void setDefaultRadiusKm(double defaultRadiusKm) {
        this.defaultRadiusKm = defaultRadiusKm;
    }

    public double getMaxRadiusKm() {
        return maxRadiusKm;
    }

    public void setMaxRadiusKm(double maxRadiusKm) {
        this.maxRadiusKm = maxRadiusKm;
    }

    public double getMinRadiusKm() {
        return minRadiusKm;
    }

    public void setMinRadiusKm(double minRadiusKm) {
        this.minRadiusKm = minRadiusKm;
    }

    public double validateRadius(Double radiusKm) {
        if (radiusKm == null) {
            return defaultRadiusKm;
        }
        if (radiusKm < minRadiusKm) {
            return minRadiusKm;
        }
        if (radiusKm > maxRadiusKm) {
            return maxRadiusKm;
        }
        return radiusKm;
    }

    public double kmToMeters(double km) {
        return km * 1000;
    }
}