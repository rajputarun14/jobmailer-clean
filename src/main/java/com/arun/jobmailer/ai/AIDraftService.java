package com.arun.jobmailer.ai;

public interface AIDraftService {
    /**
     * Tailor plain text extracted from the candidate's PDF to the job description.
     * Implementations should preserve section structure and facts; output is ATS-oriented plain text
     * (rendered later into a simple linear PDF).
     */
    String tailorResume(String resumeText, String jdText) throws Exception;
}
