package com.arun.jobmailer.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.arun.jobmailer.auth.JwtService;
import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.service.UserService;

@RestController
public class ApiAuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ApiAuthController(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        UserAccount user = userService.find(request.username());
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        List<String> roles = Arrays.stream((user.getRoles() == null ? "USER" : user.getRoles()).split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
        return ResponseEntity.ok(Map.of(
                "token", jwtService.createToken(user.getUsername(), roles),
                "tokenType", "Bearer",
                "expiresIn", 3600
        ));
    }

    @GetMapping("/api/session")
    public ResponseEntity<Map<String, Object>> session(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toList());
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "roles", roles,
                "admin", admin
        ));
    }

    public record LoginRequest(String username, String password) {}
}
