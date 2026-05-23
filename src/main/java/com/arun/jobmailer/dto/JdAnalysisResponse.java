package com.arun.jobmailer.dto;

import java.util.List;

public record JdAnalysisResponse(
        int atsScore,
        List<String> matchedSkills,
        List<String> missingKeywords
) {
    public JdAnalysisResponse {
        if (matchedSkills == null) matchedSkills = List.of();
        if (missingKeywords == null) missingKeywords = List.of();
    }
}
