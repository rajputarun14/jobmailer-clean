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
}
