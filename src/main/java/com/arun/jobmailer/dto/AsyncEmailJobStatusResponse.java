package com.arun.jobmailer.dto;

public record AsyncEmailJobStatusResponse(
        String jobId,
        String status,
        GenerateEmailResponse result,
        String error
) {}
