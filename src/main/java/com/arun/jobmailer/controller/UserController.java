package com.arun.jobmailer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.service.UserService;

@Controller
public class UserController {

    private final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> register(@RequestParam String email,
                                           @RequestParam String password,
                                           @RequestParam String gmailAppPassword) {
        try {
            String username = normalizeEmail(email);
            if (username.isBlank() || password == null || password.isBlank() || gmailAppPassword == null || gmailAppPassword.isBlank()) {
                return ResponseEntity.badRequest().body("email, login password, and Gmail app password are required");
            }
            if (!username.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return ResponseEntity.badRequest().body("valid email required");
            }
            if (userService.find(username) != null) {
                return ResponseEntity.badRequest().body("User exists");
            }
            UserAccount u = userService.createUser(username, password, gmailAppPassword, "USER");
            // If request likely comes from a browser form, redirect to login
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, "/login.html?registered");
            return ResponseEntity.status(303).headers(headers).body("User created");
        } catch (Exception e) {
            log.error("Failed to register user", e);
            return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }
}
