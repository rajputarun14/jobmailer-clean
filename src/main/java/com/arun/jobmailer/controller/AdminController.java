package com.arun.jobmailer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arun.jobmailer.model.SentEmail;
import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.repository.UserRepository;
import com.arun.jobmailer.service.SentEmailService;
import com.arun.jobmailer.service.UserService;

@RestController
public class AdminController {

    @Autowired
    private SentEmailService sentEmailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/admin/emails")
    public ResponseEntity<List<SentEmail>> allEmails() {
        return ResponseEntity.ok(sentEmailService.list());
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserAccount>> allUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/admin/users/{username}/role")
    public ResponseEntity<String> setRole(@PathVariable String username,
                                          @RequestParam String roles) {
        UserAccount u = userService.find(username);
        if (u == null) return ResponseEntity.badRequest().body("User not found");
        userService.setRoles(username, roles);
        return ResponseEntity.ok("Updated");
    }
}
