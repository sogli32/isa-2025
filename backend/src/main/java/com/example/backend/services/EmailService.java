package com.example.backend.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(String to, String token) {
        String activationLink = "http://localhost:4200/auth/activate?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Activate your account");
        message.setText("Hello,\n\nPlease click the link below to activate your account:\n" + activationLink + "\n\nThis link will expire in 24 hours.\n\nBest regards,\nYour App Team");

        mailSender.send(message);
    }
}
