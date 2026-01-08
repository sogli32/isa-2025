package com.example.backend.repository;

import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // koristi se za login (email + password)
    Optional<User> findByEmail(String email);

    // validacija prilikom registracije
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);


}
