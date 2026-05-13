package com.arun.jobmailer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.model.EmailTemplate;
import com.arun.jobmailer.model.SentEmail;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private SentEmailService sentEmailService;

    @Autowired
    private com.arun.jobmailer.service.S3Service s3Service;

    @Autowired
    private UserService userService;

    @org.springframework.beans.factory.annotation.Value("${aws.s3.use:false}")
    private boolean useS3;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    public SentEmail sendEmail(String email, EmailTemplate template, String templateName, String owner) throws Exception {
        JavaMailSenderImpl userMailSender = createUserMailSender(owner);
        MimeMessage message = userMailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(owner);
        helper.setTo(email);
        helper.setSubject(template.getSubject());
        // convert plain-text newlines into HTML paragraphs and <br/>s,
        // preserve any existing HTML tags (e.g., <b>) in the template
        String htmlBody = toHtml(template.getBody());
        // create sent record before sending so we have an id for the tracking pixel
        SentEmail sent = sentEmailService.create(email, owner, templateName, template.getSubject());
        // append tracking pixel that points back to our app
        // use a dot-free path to avoid path-variable truncation issues
        String trackImg = "<img src=\"/track/" + sent.getId() + "\" style=\"display:none;width:1px;height:1px;\"/>";
        helper.setText(htmlBody + trackImg, true);

        java.io.File tempDownload = null;
        try {
            java.io.File attachmentFile;
            if (useS3) {
                // attempt S3 download first
                tempDownload = java.io.File.createTempFile("resume-", ".pdf");
                boolean got = s3Service.downloadFile("Arun_Kumar_Resume.pdf", tempDownload);
                if (got) {
                    attachmentFile = tempDownload;
                } else {
                    // fallback to local attachment path
                    attachmentFile = new java.io.File(template.getAttachment());
                }
            } else {
                attachmentFile = new java.io.File(template.getAttachment());
            }

            FileSystemResource file = new FileSystemResource(attachmentFile);
            helper.addAttachment("Arun_Kumar_Resume.pdf", file);

            try {
                userMailSender.send(message);
                return sent;
            } catch (Exception ex) {
                try { sentEmailService.delete(sent.getId()); } catch (Exception ignore) {}
                throw ex;
            }
        } finally {
            if (tempDownload != null && tempDownload.exists()) {
                try { tempDownload.delete(); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Send a follow-up email using an existing SentEmail record (reuses tracking id),
     * but does not create a new SentEmail row.
     */
    public void sendFollowup(SentEmail sent, EmailTemplate template) throws Exception {
        JavaMailSenderImpl userMailSender = createUserMailSender(sent.getOwner());
        MimeMessage message = userMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(sent.getOwner());
        helper.setTo(sent.getRecipient());
        helper.setSubject(template.getSubject());
        String htmlBody = toHtml(template.getBody());
        String trackImg = "<img src=\"/track/" + sent.getId() + "\" style=\"display:none;width:1px;height:1px;\"/>";
        helper.setText(htmlBody + trackImg, true);

        java.io.File tempDownload = null;
        try {
            java.io.File attachmentFile;
            if (useS3) {
                tempDownload = java.io.File.createTempFile("resume-", ".pdf");
                boolean got = s3Service.downloadFile("Arun_Kumar_Resume.pdf", tempDownload);
                if (got) {
                    attachmentFile = tempDownload;
                } else {
                    attachmentFile = new java.io.File(template.getAttachment());
                }
            } else {
                attachmentFile = new java.io.File(template.getAttachment());
            }

            FileSystemResource file = new FileSystemResource(attachmentFile);
            helper.addAttachment("Arun_Kumar_Resume.pdf", file);

            userMailSender.send(message);
        } finally {
            if (tempDownload != null && tempDownload.exists()) {
                try { tempDownload.delete(); } catch (Exception ignore) {}
            }
        }
    }

    /**
     * Send email using an explicit attachment file path (uploaded resume).
     * Creates a SentEmail record as usual and uses the provided attachment path.
     */
    public SentEmail sendEmail(String email, EmailTemplate template, String templateName, String owner, String attachmentPath) throws Exception {
        JavaMailSenderImpl userMailSender = createUserMailSender(owner);
        MimeMessage message = userMailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(owner);
        helper.setTo(email);
        helper.setSubject(template.getSubject());
        String htmlBody = toHtml(template.getBody());
        SentEmail sent = sentEmailService.create(email, owner, templateName, template.getSubject());
        String trackImg = "<img src=\"/track/" + sent.getId() + "\" style=\"display:none;width:1px;height:1px;\"/>";
        helper.setText(htmlBody + trackImg, true);

        java.io.File attachmentFile = new java.io.File(attachmentPath);
        FileSystemResource file = new FileSystemResource(attachmentFile);
        helper.addAttachment(attachmentFile.getName(), file);

        try {
            userMailSender.send(message);
            return sent;
        } catch (Exception ex) {
            try { sentEmailService.delete(sent.getId()); } catch (Exception ignore) {}
            throw ex;
        }
    }

    private JavaMailSenderImpl createUserMailSender(String owner) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailHost);
        sender.setPort(mailPort);
        sender.setUsername(owner);
        sender.setPassword(userService.getGmailAppPassword(owner));
        sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        sender.getJavaMailProperties().put("mail.smtp.ssl.trust", mailHost);
        sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "5000");
        sender.getJavaMailProperties().put("mail.smtp.timeout", "5000");
        return sender;
    }

    private String toHtml(String body) {
        if (body == null) return "";
        String normalized = body.replace("\r\n", "\n").replace("\r", "\n");
        String[] paragraphs = normalized.split("\n\s*\n");
        StringBuilder sb = new StringBuilder();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            String withBreaks = trimmed.replace("\n", "<br/>");
            sb.append("<p>").append(withBreaks).append("</p>");
        }
        return sb.toString();
    }
}
