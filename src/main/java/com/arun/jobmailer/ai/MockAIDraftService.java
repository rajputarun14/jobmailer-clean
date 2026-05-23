package com.arun.jobmailer.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock")
public class MockAIDraftService implements AIDraftService {

    @Override
    public String tailorResume(String resumeText, String jdText) {
        String base = resumeText == null ? "" : resumeText;
        if (jdText == null || jdText.isBlank()) {
            return base;
        }
        return base + "\n\n---\n[Mock AI: set ai.provider=gemini and configure a Gemini key in Admin for real JD-aware tailoring. Structure-preserving prompts are used in production.]\n";
    }

    @Override
    public String draftApplicationEmail(String resumeText, String jdText, String subject, String fallbackName) {
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
}
