package com.arun.jobmailer.model;

public class EmailTemplate {

    private String subject;
    private String body;
    private String attachment;

    public EmailTemplate(String subject, String body, String attachment) {
        this.subject = subject;
        this.body = body;
        this.attachment = attachment;
    }

    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getAttachment() { return attachment; }
}