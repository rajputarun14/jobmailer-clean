package com.arun.jobmailer.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.model.SentEmail;
import com.arun.jobmailer.repository.SentEmailRepository;

@Service
public class SentEmailService {

    @Autowired
    private SentEmailRepository repo;

    public SentEmail create(String recipient, String owner, String templateName, String subject) {
        String id = UUID.randomUUID().toString();
        SentEmail s = new SentEmail(id, recipient, owner, templateName, subject, Instant.now());
        return repo.save(s);
    }

    public SentEmail get(String id) {
        return repo.findById(id).orElse(null);
    }

    public List<SentEmail> list() {
        return repo.findAllByOrderBySentAtDesc();
    }

    public List<SentEmail> listForOwner(String owner) {
        return repo.findAllByOwnerOrderBySentAtDesc(owner);
    }

    public void markOpened(String id) {
        SentEmail s = get(id);
        if (s != null) {
            s.markOpened();
            repo.save(s);
        }
    }

    public void incrementFollowups(String id) {
        SentEmail s = get(id);
        if (s != null) {
            s.incrementFollowups();
            // bump sentAt so this row appears as most recent on dashboards
            s.setSentAt(Instant.now());
            repo.save(s);
        }
    }

    public void delete(String id) {
        repo.deleteById(id);
    }
}
