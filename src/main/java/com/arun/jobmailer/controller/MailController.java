package com.arun.jobmailer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.Map;

import jakarta.mail.AuthenticationFailedException;

import com.arun.jobmailer.model.EmailTemplate;
import com.arun.jobmailer.service.EmailService;
import com.arun.jobmailer.service.TemplateService;
import com.arun.jobmailer.service.UserService;

@RestController
public class MailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private com.arun.jobmailer.service.UploadService uploadService;

    @Autowired
    private UserService userService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMail(@RequestParam String email,
                                           @RequestParam String template,
                                           @RequestParam(required=false) String resumeId,
                                           @RequestParam(required=false) String tailoredId,
                                           @RequestParam(required=false) String subject,
                                           @RequestParam(required=false) String jd,
                                           Principal principal) {
        try {
            String owner = principal == null ? "anonymous" : principal.getName();
            java.nio.file.Path attachmentPath = resolveAttachmentPath(owner, resumeId, tailoredId);
            String finalSubject = subject;
            if (finalSubject != null && !finalSubject.isBlank()) {
                userService.saveEmailSubject(owner, finalSubject);
            } else {
                finalSubject = userService.getEmailSubject(owner);
            }

            EmailTemplate t = templateService.getTemplate(template, owner, finalSubject, jd, attachmentPath);
            if (t == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Template not found: " + template);
            }

            // send and record sent email (includes tracking pixel)
            if (tailoredId != null && !tailoredId.isBlank()) {
                emailService.sendEmail(email, t, template, owner, attachmentPath.toString());
            } else if (attachmentPath != null && java.nio.file.Files.exists(attachmentPath)) {
                emailService.sendEmail(email, t, template, owner, attachmentPath.toString());
            } else {
                emailService.sendEmail(email, t, template, owner);
            }
            return ResponseEntity.ok("Email Sent!");
        } catch (MailAuthenticationException | AuthenticationFailedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("SMTP authentication failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send email: " + e.getMessage());
        }
    }

    @GetMapping("/api/email-settings")
    public ResponseEntity<Map<String, Object>> emailSettings(Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        return ResponseEntity.ok(Map.of("subject", userService.getEmailSubject(owner)));
    }

    @PostMapping("/api/email-settings")
    public ResponseEntity<Map<String, Object>> saveEmailSettings(@RequestParam(required=false) String subject,
                                                                 Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        userService.saveEmailSubject(owner, subject);
        return ResponseEntity.ok(Map.of("subject", userService.getEmailSubject(owner)));
    }

    // List sent emails for dashboard
    @GetMapping("/emails")
    public ResponseEntity<Object> listEmails(Principal principal) {
        if (isAdmin(principal)) {
            return ResponseEntity.ok(templateService.getAllSentEmails());
        }
        String owner = principal == null ? "anonymous" : principal.getName();
        return ResponseEntity.ok(templateService.getAllSentEmails(owner));
    }

    private boolean isAdmin(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private java.nio.file.Path resolveAttachmentPath(String owner, String resumeId, String tailoredId) {
        if (tailoredId != null && !tailoredId.isBlank()) {
            java.nio.file.Path p = uploadService.getTailoredResumePath(owner, tailoredId);
            if (!java.nio.file.Files.exists(p)) {
                throw new IllegalArgumentException("Tailored resume not found");
            }
            return p;
        }
        if (resumeId != null && !resumeId.isBlank()) {
            java.nio.file.Path p = uploadService.getResumePath(owner, resumeId);
            if (!java.nio.file.Files.exists(p)) {
                throw new IllegalArgumentException("Uploaded resume not found");
            }
            return p;
        }
        java.nio.file.Path current = uploadService.getCurrentResumePath(owner);
        if (current != null && java.nio.file.Files.exists(current)) {
            return current;
        }
        return null;
    }

    // Tracking pixel endpoint - marks email opened and returns a 1x1 PNG
    @GetMapping(value = "/track/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> trackOpen(@org.springframework.web.bind.annotation.PathVariable String id) {
        templateService.markEmailOpened(id);
        byte[] png = new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,6,0,0,0,31,21,(byte)196,(byte)137,0,0,0,12,73,68,65,84,8,29,99,0,1,0,0,5,0,1,(byte)225,65,101,(byte)182,0,0,0,0,73,69,78,68,(byte)174,66,96,(byte)130};
        return ResponseEntity.ok().header("Cache-Control", "no-cache, no-store, must-revalidate").body(png);
    }

    // Send a follow-up for a sent email
    @PostMapping("/emails/{id}/followup")
    public ResponseEntity<String> followup(@org.springframework.web.bind.annotation.PathVariable String id,
                                           @RequestParam(required=false) String template,
                                           Principal principal) {
        try {
            String owner = principal == null ? "anonymous" : principal.getName();
            boolean ok = templateService.sendFollowup(id, template, owner);
            if (ok) return ResponseEntity.ok("Follow-up sent");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not send follow-up");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
