package com.example.backend.security;

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

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("🔍 JWT Filter: " + method + " " + path);
        
        // PRESKOČI VALIDACIJU ZA JAVNE ENDPOINTE
        if (shouldNotFilter(request)) {
            System.out.println("✅ Skipping JWT validation (public endpoint)");
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
                    System.out.println("✅ JWT validated for user: " + username);
                }
            } catch (Exception e) {
                System.err.println("❌ JWT validation failed: " + e.getMessage());
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