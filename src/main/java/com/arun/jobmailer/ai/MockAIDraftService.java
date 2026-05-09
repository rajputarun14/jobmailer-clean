package com.arun.jobmailer.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAIDraftService implements AIDraftService {

    @Override
    public String tailorResume(String resumeText, String jdText) {
        // Very simple mock: keep headings, append JD keywords line, and ensure basic ATS-friendly layout
        StringBuilder sb = new StringBuilder();
        sb.append("Summary:\n");
        sb.append("Tailored resume summary emphasizing relevant skills.\n\n");
        sb.append("Experience:\n");
        // include first 10 lines of original resume as mock
        String[] lines = resumeText.split("\\r?\\n");
        int count = 0;
        for (String l : lines) {
            if (l.trim().isEmpty()) continue;
            sb.append("- ").append(l.trim()).append("\n");
            if (++count >= 10) break;
        }
        sb.append("\nSkills:\n");
        if (jdText != null && !jdText.isBlank()) {
            // extract some keywords (naive)
            String[] words = jdText.replaceAll("[^a-zA-Z0-9 ]", " ").split("\\s+");
            int k = 0;
            for (String w : words) {
                if (w.length() > 3) {
                    sb.append(w.toLowerCase()).append(", ");
                    if (++k >= 10) break;
                }
            }
            sb.append("\n");
        } else {
            sb.append("Java, Spring Boot, SQL\n");
        }
        sb.append("\nNote: This is a mock tailored resume. Replace with real AI provider for production.\n");
        return sb.toString();
    }
}
