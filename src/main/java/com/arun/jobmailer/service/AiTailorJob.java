package com.arun.jobmailer.service;

import java.time.Instant;

public class AiTailorJob {

    public enum Status {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    private final String id;
    private final String owner;
    private final Instant createdAt;
    private volatile Status status;
    private volatile String tailoredId;
    private volatile String downloadUrl;
    private volatile String error;
    /** Non-fatal completion message for the client (e.g. how the PDF is structured). */
    private volatile String info;

    public AiTailorJob(String id, String owner) {
        this.id = id;
        this.owner = owner;
        this.createdAt = Instant.now();
        this.status = Status.QUEUED;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getTailoredId() { return tailoredId; }
    public void setTailoredId(String tailoredId) { this.tailoredId = tailoredId; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
}
