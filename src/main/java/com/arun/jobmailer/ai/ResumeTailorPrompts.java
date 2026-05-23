package com.arun.jobmailer.ai;

/**
 * Shared "Mirror &amp; Match" instructions for tailoring resume text to a JD (Gemini + OpenAI).
 * The app renders this plain text into a simple PDF; structural mirroring is enforced in the text stream.
 */
public final class ResumeTailorPrompts {

    private ResumeTailorPrompts() {
    }

    /**
     * System-style instructions (OpenAI system message, or folded into Gemini user prompt).
     */
    public static final String INSTRUCTIONS = """
            MIRROR & MATCH — PROFESSIONAL RESUME REWRITE

            You are an expert Career Coach and Technical Writer. Your task is to rewrite a resume to align perfectly with a specific Job Description (JD) while strictly adhering to these rules:

            1. STRATEGIC INTEGRATION: Identify key skills, tools, and methodologies in the JD that are supported by the candidate's history but might be missing or under-emphasized. Integrate them naturally into 'Technical Skills' and 'Professional Experience' sections.

            2. CONTEXTUAL TWEAKING: Rewrite existing bullet points to use the specific terminology found in the JD (e.g., if the JD mentions 'high-throughput systems' and the resume mentions 'scalable APIs,' update the phrasing to match the JD exactly).

            3. STRICT STRUCTURAL MIRROR: Do NOT change the layout, section order, or overall organization. The output must be a direct textual mirror of the ORIGINAL RESUME's structure (Experience, Education, Projects, etc., in the same order). Use the same bullet styles.

            4. IMPACT DRIVEN: Ensure all updated descriptions use strong action verbs and, where the original resume supports it, emphasize quantifiable outcomes to increase ATS scores.

            5. NO FABRICATIONS: Use the original resume as the sole SOURCE OF TRUTH for employment dates, job titles, companies, and degrees. Do not invent new roles or achievements.

            6. OUTPUT FORMAT: Return ONLY the rewritten resume as plain UTF-8 text. Do not include Markdown code fences, preambles, or commentary. Start immediately with the resume content.
            """;

    public static String originalResumeSection(String resumeText) {
        String r = resumeText == null ? "" : resumeText;
        return "ORIGINAL RESUME (plain text from uploaded PDF):\n" + r;
    }

    public static String jobDescriptionSection(String jdText) {
        String j = jdText == null ? "" : jdText;
        return "JOB DESCRIPTION:\n" + j;
    }

    /** Single user payload for OpenAI (system message carries INSTRUCTIONS). */
    public static String openAiUserContent(String resumeText, String jdText) {
        return originalResumeSection(resumeText) + "\n\n" + jobDescriptionSection(jdText);
    }

    /** One combined prompt for Gemini (instructions + resume + JD). */
    public static String geminiFullPrompt(String resumeText, String jdText) {
        return INSTRUCTIONS + "\n\n---\n\n"
                + originalResumeSection(resumeText) + "\n\n---\n\n"
                + jobDescriptionSection(jdText) + "\n\n---\n\n"
                + "INSTRUCTION: Completely rewrite the ORIGINAL RESUME provided above. "
                + "Do not append notes. Do not provide a summary of changes. "
                + "Output the full, rewritten resume text from top to bottom, mirroring the original structure exactly.";
    }

    public static String emailDraftPrompt(String resumeText, String jdText, String subject, String fallbackName) {
        String safeSubject = subject == null ? "" : subject;
        String safeName = fallbackName == null ? "Candidate" : fallbackName;
        return """
                You are an expert career email writer. Draft a short, high-conversion email body for a job application/referral request.

                Rules:
                1. Use the resume as the source of truth. Do not invent employers, years, skills, metrics, or achievements.
                2. Match the Job Description when it is provided, but only using skills and experience supported by the resume.
                3. Keep the body concise: greeting, 2 short paragraphs, attachment/referral ask, thanks, and sign-off.
                4. Start exactly with "Hi, I hope you're doing well." Do not use Dear. Do not mention hiring manager names.
                5. Do not write the subject line. Do not include markdown, bullets, code fences, bracketed placeholders, or text like [Hiring Manager Name].
                6. Include 2-4 concrete skills, tools, domains, company names, or outcomes from the resume.
                7. End with "Best regards" followed by the candidate name from the resume. If unsure, use this fallback name: %s.

                Email subject/context:
                %s

                %s

                %s
                """.formatted(safeName, safeSubject, originalResumeSection(resumeText), jobDescriptionSection(jdText));
    }

    public static String structuredEmailPrompt(String resumeText, String jdText, String subject, String fallbackName) {
        String safeSubject = subject == null ? "" : subject;
        String safeName = fallbackName == null ? "Candidate" : fallbackName;
        return """
                You are a senior technical recruiter and career writer.

                Generate a personalized cold outreach package from the candidate resume and JD.

                Strict rules:
                - Return ONLY valid JSON. No markdown and no commentary.
                - JSON keys must be: subject, emailBody, linkedinMessage, atsScore, matchedSkills, missingKeywords.
                - emailBody must be under 180 words.
                - Start emailBody with: "Hi, I hope you're doing well."
                - Do not use "Dear", bracket placeholders, or names like [Hiring Manager Name].
                - Sound professional, concise, confident, recruiter-friendly, and human-written.
                - Avoid weak phrases like "if my profile looks suitable" and avoid sounding desperate.
                - Mention matching skills, years of experience, backend/system design experience, and relevant projects or domains only when present in the resume.
                - Prefer this structure: greeting, reaching out regarding role, experience + strongest matching skills, current work/projects hook, resume/next-step ask, thanks, sign-off.
                - Do not hallucinate skills, companies, projects, metrics, employers, or years that are not present in the resume.
                - Prioritize JD keywords that are actually present in the resume.
                - linkedinMessage must be shorter than 450 characters.
                - atsScore must be an integer from 0 to 100 based on overlap between resume and JD.
                - matchedSkills and missingKeywords must be arrays of concise strings.
                - End emailBody with "Best regards" followed by the candidate name from the resume. If unsure, use: %s.

                Existing subject, if user supplied one:
                %s

                %s

                %s
                """.formatted(safeName, safeSubject, originalResumeSection(resumeText), jobDescriptionSection(jdText));
    }
}
