package com.arun.jobmailer.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @Column(length = 100)
    private String username;

    private String password;

    @Column(name = "encrypted_gmail_app_password", length = 2048)
    private String encryptedGmailAppPassword;

    // comma-separated roles, e.g. ROLE_USER,ROLE_ADMIN
    private String roles;

    private boolean enabled = true;

    private Instant createdAt;

    public UserAccount() {}

    public UserAccount(String username, String password, String roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.enabled = true;
        this.createdAt = Instant.now();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEncryptedGmailAppPassword() { return encryptedGmailAppPassword; }
    public void setEncryptedGmailAppPassword(String encryptedGmailAppPassword) { this.encryptedGmailAppPassword = encryptedGmailAppPassword; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
