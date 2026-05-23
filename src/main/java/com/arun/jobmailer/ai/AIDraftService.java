package com.arun.jobmailer.ai;

import java.util.List;

import com.arun.jobmailer.dto.GenerateEmailResponse;

public interface AIDraftService {
    /**
     * Tailor plain text extracted from the candidate's PDF to the job description.
     * Implementations should preserve section structure and facts; output is ATS-oriented plain text
     * (rendered later into a simple linear PDF).
     */
    String tailorResume(String resumeText, String jdText) throws Exception;

    /**
     * Draft a concise recruiter/referral email body from the candidate's resume and JD.
     * Implementations should keep it short, factual, and use the resume as the source of truth.
     */
    default String draftApplicationEmail(String resumeText, String jdText, String subject, String fallbackName) throws Exception {
        String name = (fallbackName == null || fallbackName.isBlank()) ? "Candidate" : fallbackName;
        return """
                Hi, I hope you're doing well.

                I am interested in this opportunity and believe my experience aligns well with the role. I have attached my resume for your review.

                I would sincerely appreciate your support with a referral or next steps if my profile is suitable.

                Thank you for your time and consideration.

                Best regards
                %s
                """.formatted(name);
    }

    default GenerateEmailResponse generateApplicationEmail(String resumeText, String jdText, String subject, String fallbackName) throws Exception {
        String body = draftApplicationEmail(resumeText, jdText, subject, fallbackName);
        String finalSubject = (subject == null || subject.isBlank()) ? "Application for Software Engineering Role" : subject.trim();
        return new GenerateEmailResponse(
                finalSubject,
                body,
                "Hi, I came across this opportunity and believe my experience aligns well. I would be grateful if you could review my profile or refer me for a suitable role. Thanks!",
                60,
                List.of(),
                List.of()
        );
    }
}
