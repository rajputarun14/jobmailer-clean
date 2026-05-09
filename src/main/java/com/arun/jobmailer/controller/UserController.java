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
    public ResponseEntity<String> register(@RequestParam String username,
                                           @RequestParam String password) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body("username and password required");
            }
            if (userService.find(username) != null) {
                return ResponseEntity.badRequest().body("User exists");
            }
            UserAccount u = userService.createUser(username, password, "USER");
            // If request likely comes from a browser form, redirect to login
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.LOCATION, "/login.html?registered");
            return ResponseEntity.status(303).headers(headers).body("User created");
        } catch (Exception e) {
            log.error("Failed to register user", e);
            return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
        }
    }
}
