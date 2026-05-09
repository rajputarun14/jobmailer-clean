package com.arun.jobmailer.ai;

public interface AIDraftService {
    /**
     * Tailor the plain-text resume to the job description.
     * Return the tailored resume text (ATS-friendly plain text) or throw Exception on error.
     */
    String tailorResume(String resumeText, String jdText) throws Exception;
}
