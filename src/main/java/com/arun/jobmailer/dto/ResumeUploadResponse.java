package com.arun.jobmailer.dto;

public record ResumeUploadResponse(
        boolean hasResume,
        String id,
        String name,
        String fileType
) {}
