package com.example.backend.security;

import com.example.backend.services.ActiveUsersMetricService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ActiveUsersMetricService activeUsersMetricService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, ActiveUsersMetricService activeUsersMetricService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.activeUsersMetricService = activeUsersMetricService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // PRESKOČI VALIDACIJU ZA JAVNE ENDPOINTE
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // VALIDIRAJ JWT TOKEN
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String username = jwtUtil.validateToken(token).getBody().getSubject();

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Belezi aktivnost korisnika za metriku "active_users_24h"
                    activeUsersMetricService.recordUserActivity(username);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Preskoči JWT za sve OPTIONS (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // Preskoči JWT za auth endpoints
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // Preskoči JWT za WebSocket
        if (path.startsWith("/ws-chat")) {
            return true;
        }

        // Preskoči JWT za Actuator
        if (path.startsWith("/actuator")) {
            return true;
        }

        // Preskoči JWT za load test
        if (path.startsWith("/api/load-test")) {
            return true;
        }

        // Preskoči JWT za geolocation i benchmark
        if (path.startsWith("/api/geolocation") || path.startsWith("/api/benchmark")) {
            return true;
        }

        // Preskoči JWT za GET videos i comments
        if ("GET".equals(method) && (path.startsWith("/api/videos") || path.startsWith("/api/comments"))) {
            return true;
        }

        return false;
    }
}
