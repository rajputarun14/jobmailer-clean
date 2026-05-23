package com.arun.jobmailer.dto;

import java.util.List;

public record GenerateEmailResponse(
        String subject,
        String emailBody,
        String linkedinMessage,
        int atsScore,
        List<String> matchedSkills,
        List<String> missingKeywords
) {
    public GenerateEmailResponse {
        if (matchedSkills == null) matchedSkills = List.of();
        if (missingKeywords == null) missingKeywords = List.of();
    }
}
