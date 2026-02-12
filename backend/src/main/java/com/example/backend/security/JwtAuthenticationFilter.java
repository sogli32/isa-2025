package com.example.backend.security;

import com.example.backend.services.ActiveUsersMetricService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ActiveUsersMetricService activeUsersMetricService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ActiveUsersMetricService activeUsersMetricService) {
        this.jwtUtil = jwtUtil;
        this.activeUsersMetricService = activeUsersMetricService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                var claims = jwtUtil.validateToken(token).getBody();
                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);

                // Belezi aktivnost korisnika za metriku "active_users_24h"
                activeUsersMetricService.recordUserActivity(email);

            } catch (Exception e) {
                // Ne blokiraj request - samo nemoj postaviti autentifikaciju.
                // Spring Security ce sam odluciti da li endpoint zahteva auth.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
