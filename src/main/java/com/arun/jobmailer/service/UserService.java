package com.arun.jobmailer.service;

import java.util.Arrays;

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

    public UserAccount createUser(String username, String rawPassword, String roles) {
        String hashed = passwordEncoder.encode(rawPassword);
        UserAccount u = new UserAccount(username, hashed, roles);
        return repo.save(u);
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
}
