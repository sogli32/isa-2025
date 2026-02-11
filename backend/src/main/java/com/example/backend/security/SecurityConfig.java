package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
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
                        // Dozvoli OPTIONS preflight za sve endpointe (CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // javni endpointi
                        .requestMatchers("/hello").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/videos").permitAll()
                        .requestMatchers("/api/videos/*/thumbnail").permitAll()
                        .requestMatchers("/api/videos/*/stream").permitAll()
                        .requestMatchers("/api/videos/*/stream-info").permitAll()
                        .requestMatchers("/api/videos/*").permitAll()
                        .requestMatchers("/api/videos/*/view").permitAll()
                        .requestMatchers("/api/comments/*").permitAll() // GET komentari svi
                        // POST komentar treba autentifikaciju
                        .requestMatchers("/api/benchmark/**").permitAll()
                        .requestMatchers("/api/geolocation/**").permitAll()
                        .requestMatchers("/api/etl/**").authenticated()
                        .requestMatchers("/api/comments/*").authenticated()
                        .requestMatchers("/api/videos/*/like").authenticated()
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
