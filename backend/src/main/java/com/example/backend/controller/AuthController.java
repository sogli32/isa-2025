package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.security.JwtUtil;
import com.example.backend.services.LoginAttemptService;
import com.example.backend.services.UserService;
import com.example.backend.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService,
                          LoginAttemptService loginAttemptService,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.jwtUtil = jwtUtil;
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        String ip = IpUtil.getClientIp(httpRequest);

        if (loginAttemptService.isBlocked(ip)) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Previše pokušaja prijave. Pokušajte ponovo za minut."));
        }

        return userService.login(request.getEmail(), request.getPassword())
                .map(user -> {
                    loginAttemptService.loginSucceeded(ip);

                    // generišemo JWT token
                    String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

                    return ResponseEntity.ok(Map.of(
                            "token", token,
                            "username", user.getUsername(),
                            "role", user.getRole()
                    ));
                })
                .orElseGet(() -> {
                    loginAttemptService.loginFailed(ip);
                    return ResponseEntity.status(401)
                            .body(Map.of("error", "Pogrešan email ili lozinka"));
                });
    }

    // ===== REGISTER =====
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);

            return ResponseEntity.ok(Map.of(
                    "username", user.getUsername(),
                    "role", user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ===== ACCOUNT ACTIVATION =====
    @GetMapping("/activate")
    public ResponseEntity<Map<String, String>> activateAccount(@RequestParam("token") String token) {
        try {
            userService.activateUser(token);
            return ResponseEntity.ok(Map.of(
                    "message", "Nalog aktiviran! Sada se možete prijaviti."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
