package com.example.backend.services;

import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.model.VerificationToken;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       VerificationTokenRepository tokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    // ===== LOGIN =====
    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(password) && user.isEnabled());
    }

    // ===== REGISTER SA TOKENOM =====
    @Transactional
    public User register(RegisterRequest request) {

        if (isNullOrEmpty(request.getUsername()) ||
                isNullOrEmpty(request.getEmail()) ||
                isNullOrEmpty(request.getPassword()) ||
                isNullOrEmpty(request.getConfirmPassword()) ||
                isNullOrEmpty(request.getFirstName()) ||
                isNullOrEmpty(request.getLastName()) ||
                isNullOrEmpty(request.getAddress())) {
            throw new IllegalArgumentException("All fields are required");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must have at least 8 characters");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // kasnije hash
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setRole("USER");
        user.setEnabled(false);

        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user, LocalDateTime.now().plusHours(24));
        tokenRepository.save(verificationToken);

        emailService.sendActivationEmail(user.getEmail(), token);

        return user;
    }

    // ===== AKTIVACIJA NALOGA =====
    @Transactional
    public boolean activateUser(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation token"));

        User user = verificationToken.getUser();

        if (user.isEnabled()) {
            return true;
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Activation token has expired");
        }

        user.setEnabled(true);
        userRepository.save(user);
        return true;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
