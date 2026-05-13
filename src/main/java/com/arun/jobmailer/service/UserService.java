package com.arun.jobmailer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CredentialEncryptionService credentialEncryptionService;

    public UserService() {}

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder, CredentialEncryptionService credentialEncryptionService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    public UserAccount createUser(String username, String rawPassword, String roles) {
        String hashed = passwordEncoder.encode(rawPassword);
        UserAccount u = new UserAccount(username, hashed, roles);
        return repo.save(u);
    }

    public UserAccount createUser(String email, String rawPassword, String gmailAppPassword, String roles) {
        String normalizedEmail = normalizeEmail(email);
        String hashed = passwordEncoder.encode(rawPassword);
        UserAccount u = new UserAccount(normalizedEmail, hashed, roles);
        u.setEncryptedGmailAppPassword(credentialEncryptionService.encrypt(gmailAppPassword));
        return repo.save(u);
    }

    public String getGmailAppPassword(String username) {
        UserAccount user = find(username);
        if (user == null || user.getEncryptedGmailAppPassword() == null || user.getEncryptedGmailAppPassword().isBlank()) {
            throw new IllegalStateException("Gmail app password is not configured for this user");
        }
        return credentialEncryptionService.decrypt(user.getEncryptedGmailAppPassword());
    }

    public UserAccount find(String username) {
        return repo.findById(username).orElse(null);
    }

    public java.util.List<UserAccount> listAll() {
        return repo.findAll();
    }

    public void setRoles(String username, String roles) {
        UserAccount u = find(username);
        if (u == null) return;
        u.setRoles(roles);
        repo.save(u);
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }
}
