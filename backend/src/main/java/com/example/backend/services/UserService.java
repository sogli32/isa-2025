package com.example.backend.services;

import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ===== LOGIN =====
    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(password));
    }

    // ===== REGISTER SA VALIDACIJAMA =====
    public User register(RegisterRequest request) {

        // 1. Provera obaveznih polja
        if (isNullOrEmpty(request.getUsername()) ||
                isNullOrEmpty(request.getEmail()) ||
                isNullOrEmpty(request.getPassword()) ||
                isNullOrEmpty(request.getConfirmPassword()) ||
                isNullOrEmpty(request.getFirstName()) ||
                isNullOrEmpty(request.getLastName()) ||
                isNullOrEmpty(request.getAddress())) {

            throw new IllegalArgumentException("All fields are required");
        }

        // 2. Provera da li se lozinke poklapaju
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 3. Minimalna dužina lozinke
        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must have at least 8 characters");
        }

        // 4. Jedinstvenost username-a
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 5. Jedinstvenost email-a
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 6. Kreiranje korisnika
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // heširanje dolazi kasnije
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setRole("USER");

        return userRepository.save(user);
    }

    // ===== POMOĆNA METODA =====
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
