package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Dozvoli zahteve sa frontend-a
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://127.0.0.1:4200"
        ));
        
        // Dozvoli sve HTTP metode
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Dozvoli sve header-e
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Dozvoli credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Primeni konfiguraciju na sve endpoint-e
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}