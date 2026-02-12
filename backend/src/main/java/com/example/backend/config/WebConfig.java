package com.example.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

@Configuration
public class WebConfig {

    /**
     * CORS Filter sa najvišim prioritetom - izvršava se PRE svega
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> customCorsFilter() {  // ← PROMENJEN NAZIV
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorsFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        System.out.println("✅ CORS Filter registered with HIGHEST_PRECEDENCE");
        
        return registrationBean;
    }

    /**
     * Custom CORS Filter
     */
    public static class CorsFilter implements Filter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            String origin = request.getHeader("Origin");
            
            // Dozvoli localhost:4200 i 127.0.0.1:4200
            if (origin != null && (origin.equals("http://localhost:4200") || origin.equals("http://127.0.0.1:4200"))) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                response.setHeader("Access-Control-Allow-Headers", "*");
                response.setHeader("Access-Control-Max-Age", "3600");
                
                System.out.println("🌐 CORS headers added for: " + request.getMethod() + " " + request.getRequestURI());
            }

            // Ako je OPTIONS (preflight), vrati 200 OK odmah
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                System.out.println("✅ OPTIONS preflight handled for: " + request.getRequestURI());
                return;
            }

            chain.doFilter(req, res);
        }
    }
}