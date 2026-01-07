package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        return userService.login(username, password)
                .map(user -> ResponseEntity.ok(Map.of(
                        "username", user.getUsername(),
                        "role", user.getRole()
                )))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of(
                        "error", "Invalid credentials"
                )));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.get("role");

        User user = userService.register(username, password, role);
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

}
