package com.arun.jobmailer.dto;

public record GenerateEmailRequest(
        String resumeId,
        String jd,
        String subject
) {}
