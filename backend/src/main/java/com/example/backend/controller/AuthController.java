package com.example.backend.controller;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.services.LoginAttemptService;
import com.example.backend.services.UserService;
import com.example.backend.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(
            UserService userService,
            LoginAttemptService loginAttemptService
    ) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = IpUtil.getClientIp(httpRequest);

        // 1️⃣ Provera da li je IP blokiran
        if (loginAttemptService.isBlocked(ip)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Previše pokušaja prijave. Pokušajte ponovo za minut."
                    ));
        }

        // 2️⃣ Pokušaj prijave
        return userService.login(request.getEmail(), request.getPassword())
                .map(user -> {
                    // uspešan login → reset pokušaja
                    loginAttemptService.loginSucceeded(ip);

                    return ResponseEntity.ok(Map.of(
                            "username", user.getUsername(),
                            "role", user.getRole()
                    ));
                })
                .orElseGet(() -> {
                    // neuspešan login → uvećaj brojač
                    loginAttemptService.loginFailed(ip);

                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of(
                                    "error", "Invalid email or password"
                            ));
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
    public ResponseEntity<Map<String, String>> activateAccount(
            @RequestParam("token") String token
    ) {
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
