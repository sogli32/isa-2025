package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS requests (CORS preflight) - UVEK DOZVOLI
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // AUTH endpoints - JAVNI
                        .requestMatchers("/api/auth/**").permitAll()

                        // WebSocket
                        .requestMatchers("/ws-chat/**").permitAll()

                        // Actuator / Prometheus monitoring
                        .requestMatchers("/actuator/**").permitAll()

                        // Load test endpointi
                        .requestMatchers("/api/load-test/**").permitAll()

                        // Videos - JAVNI
                        .requestMatchers(HttpMethod.GET, "/api/videos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/videos").authenticated()

                        // Comments - GET javno, POST authenticated
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated()

                        // Geolocation & Benchmark - JAVNI
                        .requestMatchers("/api/geolocation/**").permitAll()
                        .requestMatchers("/api/benchmark/**").permitAll()

                        // ETL - authenticated
                        .requestMatchers("/api/etl/**").authenticated()

                        // Likes - authenticated
                        .requestMatchers("/api/videos/*/like").authenticated()

                        // Sve ostalo - authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
