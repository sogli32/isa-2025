package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class HelloController {

    @GetMapping("/hello") // endpoint će biti dostupan na http://localhost:8080/hello
    public String hello() {
        return "Hello, Spring Boot is working!";
    }
}
