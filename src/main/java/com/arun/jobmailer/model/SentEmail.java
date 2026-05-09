package com.arun.jobmailer.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sent_email")
public class SentEmail {

    @Id
    @Column(length = 36)
    private String id;

    private String recipient;
    private String owner;
    private String templateName;
    private String subject;
    private Instant sentAt;
    private boolean opened;
    private Instant openedAt;
    private int followupsSent;

    public SentEmail() {}

    public SentEmail(String id, String recipient, String owner, String templateName, String subject, Instant sentAt) {
        this.id = id;
        this.recipient = recipient;
        this.owner = owner;
        this.templateName = templateName;
        this.subject = subject;
        this.sentAt = sentAt;
        this.opened = false;
        this.followupsSent = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public boolean isOpened() { return opened; }
    public void setOpened(boolean opened) { this.opened = opened; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public int getFollowupsSent() { return followupsSent; }
    public void setFollowupsSent(int followupsSent) { this.followupsSent = followupsSent; }

    public void markOpened() {
        if (!this.opened) {
            this.opened = true;
            this.openedAt = Instant.now();
        }
    }

    public void incrementFollowups() {
        this.followupsSent++;
    }
}
