package com.arun.jobmailer.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.model.EmailTemplate;
import com.arun.jobmailer.model.SentEmail;

@Service
public class TemplateService {

    @Autowired
    private SentEmailService sentEmailService;

    @Autowired
    private EmailService emailService;

    public EmailTemplate getTemplate(String name) {
        if (name.equals("HR")) {
                return new EmailTemplate(
                    "SDE-2 | Java Backend Developer role | 4.5+ YOE",
                    """
        <p>Hi, I hope you’re doing well.</p>

        <p>I am interested in the <b>SDE-2/Java Backend Developer role</b>. I am currently working as a <b>Senior Software Engineer at Accolite (Bounteous x Accolite)</b> with over 5+ years of experience in backend development.</p>

        <p>My primary expertise lies in <b>Java, Spring Boot, and microservices architecture</b>. I have hands-on experience designing and building <b>scalable REST APIs</b>, working with relational databases such as MySQL and PostgreSQL, and implementing performance optimisations in distributed systems.</p>

        <p>I also have a strong foundation in Data Structures & Algorithms, along with practical experience in High- and Low-Level System Design. Additionally, I have worked with C++, C#, and caching technologies like <b>Redis</b> in production environments.</p>

        <p>I have attached my resume for your review. I would sincerely appreciate your support with a referral for a suitable SDE-2 opportunity. Please let me know if you need any additional information from my side.</p>

        <p>Thank you for your time and consideration.</p>

        <p>Best regards<br/>Arun Kumar</p>
        """,
                    "resume/Arun__Kumar.pdf"
                );
        }

        if (name.equals("sde")) {
            return new EmailTemplate(
                    "Software Engineer Application",
                    """
Hi,

I am a backend engineer with 4+ years experience in distributed systems.

Would love to explore opportunities with your team.

Regards
Arun Kumar
""",
                    "resume/Arun__Kumar.pdf"
            );
        }

        if (name.equals("referral")) {
            return new EmailTemplate(
                    "Request for Referral",
                    """
Hi,

I noticed openings at your company and wanted to request a referral.

I have 4+ years experience in Java backend development.

Regards
Arun Kumar
""",
                    "resume/Arun__Kumar.pdf"
            );
        }

        return null;
    }

    public List<SentEmail> getAllSentEmails() {
        return sentEmailService.list();
    }

    public List<SentEmail> getAllSentEmails(String owner) {
        return sentEmailService.listForOwner(owner);
    }

    public void markEmailOpened(String id) {
        sentEmailService.markOpened(id);
    }

    public boolean sendFollowup(String id, String templateName, String owner) throws Exception {
        SentEmail s = sentEmailService.get(id);
        if (s == null) return false;
        if (!owner.equals(s.getOwner())) return false; // only owner can follow-up
        String tmpl = templateName;
        if (tmpl == null || tmpl.isBlank()) tmpl = s.getTemplateName();
        EmailTemplate t = getTemplate(tmpl);
        if (t == null) return false;
        // send follow-up reusing the existing SentEmail record (so we don't create a new row)
        emailService.sendFollowup(s, t);
        sentEmailService.incrementFollowups(id);
        return true;
    }
}