package com.arun.jobmailer.service;

import com.arun.jobmailer.dto.GenerateEmailResponse;

public class AsyncEmailJob {
    private final String id;
    private final String owner;
    private volatile String status = "QUEUED";
    private volatile GenerateEmailResponse result;
    private volatile String error;

    public AsyncEmailJob(String id, String owner) {
        this.id = id;
        this.owner = owner;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public GenerateEmailResponse getResult() { return result; }
    public void setResult(GenerateEmailResponse result) { this.result = result; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
