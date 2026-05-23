package com.arun.jobmailer.dto;

public record AsyncEmailJobResponse(
        String jobId,
        String status
) {}
