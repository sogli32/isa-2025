package com.example.backend.config;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {

            // ===== USER =====
            if (!userRepository.existsByEmail("user@mail.com")) {
                User user = new User();
                user.setUsername("user");
                user.setEmail("user@mail.com");
                user.setPassword("user123"); // hashiranje dolazi kasnije
                user.setFirstName("Test");
                user.setLastName("User");
                user.setAddress("Test Address 1");
                user.setRole("USER");

                userRepository.save(user);
            }

            // ===== ADMIN =====
            if (!userRepository.existsByEmail("admin@mail.com")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@mail.com");
                admin.setPassword("admin123"); // hashiranje dolazi kasnije
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setAddress("Admin Address");
                admin.setRole("ADMIN");

                userRepository.save(admin);
            }
        };
    }
}
