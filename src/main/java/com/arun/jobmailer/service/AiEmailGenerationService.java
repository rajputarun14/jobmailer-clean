package com.arun.jobmailer.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.ai.AIDraftService;
import com.arun.jobmailer.dto.GenerateEmailResponse;
import com.arun.jobmailer.dto.JdAnalysisResponse;

@Service
public class AiEmailGenerationService {

    private static final Pattern YEARS_PATTERN = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\+?\\s*(?:years|yrs|yoe)");
    private static final String[] KNOWN_SKILLS = {
            "Java", "Spring Boot", "Microservices", "REST APIs", "MySQL", "PostgreSQL",
            "Redis", "Kafka", "AWS", "Docker", "Kubernetes", "System Design",
            "Data Structures", "Algorithms", "C++", "C#", "React", "Angular",
            "Python", "Node.js", "Distributed Systems", "Hibernate", "JPA",
            "MongoDB", "CI/CD", "Git", "Azure", "GCP", "JWT", "Authentication",
            "Caching", "Performance Optimization"
    };

    private final AIDraftService aiDraftService;
    private final UploadService uploadService;
    private final ResumeParserService resumeParserService;
    private final Executor aiTaskExecutor;
    private final Map<String, AsyncEmailJob> jobs = new ConcurrentHashMap<>();

    public AiEmailGenerationService(AIDraftService aiDraftService,
                                    UploadService uploadService,
                                    ResumeParserService resumeParserService,
                                    @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.aiDraftService = aiDraftService;
        this.uploadService = uploadService;
        this.resumeParserService = resumeParserService;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    public GenerateEmailResponse generate(String owner, String resumeId, String jd, String subject) throws Exception {
        Path resumePath = resolveResumePath(owner, resumeId);
        String resumeText = resumeParserService.extractText(resumePath);
        return generateFromText(resumeText, jd, subject, fallbackName(owner, resumeText));
    }

    public JdAnalysisResponse analyze(String owner, String resumeId, String jd) throws Exception {
        GenerateEmailResponse response = generate(owner, resumeId, jd, "");
        return new JdAnalysisResponse(response.atsScore(), response.matchedSkills(), response.missingKeywords());
    }

    public AsyncEmailJob submit(String owner, String resumeId, String jd, String subject) {
        AsyncEmailJob job = new AsyncEmailJob(UUID.randomUUID().toString(), owner);
        jobs.put(job.getId(), job);
        aiTaskExecutor.execute(() -> {
            job.setStatus("RUNNING");
            try {
                job.setResult(generate(owner, resumeId, jd, subject));
                job.setStatus("COMPLETED");
            } catch (Exception e) {
                job.setError(e.getMessage());
                job.setStatus("FAILED");
            }
        });
        return job;
    }

    public AsyncEmailJob getForOwner(String jobId, String owner) {
        AsyncEmailJob job = jobs.get(jobId);
        if (job == null || !job.getOwner().equals(owner)) return null;
        return job;
    }

    GenerateEmailResponse generateFromText(String resumeText, String jd, String subject, String fallbackName) {
        String candidateName = titleName(fallbackName);
        JdAnalysisResponse analysis = analyzeText(resumeText, jd);
        try {
            GenerateEmailResponse ai = aiDraftService.generateApplicationEmail(resumeText, jd, subject, candidateName);
            GenerateEmailResponse cleaned = normalize(ai, analysis, resumeText, jd, subject, candidateName);
            if (isUsable(cleaned, resumeText)) return cleaned;
        } catch (Exception ignored) {
            // Use deterministic response when AI is unavailable or returns invalid JSON/content.
        }
        return fallbackResponse(resumeText, jd, subject, candidateName, analysis);
    }

    private Path resolveResumePath(String owner, String resumeId) {
        Path path = (resumeId == null || resumeId.isBlank())
                ? uploadService.getCurrentResumePath(owner)
                : uploadService.getResumePath(owner, resumeId);
        if (path == null || !Files.exists(path)) {
            throw new IllegalArgumentException("Resume not found. Upload a resume first.");
        }
        return path;
    }

    private GenerateEmailResponse normalize(GenerateEmailResponse response,
                                            JdAnalysisResponse analysis,
                                            String resumeText,
                                            String jd,
                                            String subject,
                                            String fallbackName) {
        String finalSubject = cleanSubject(response.subject());
        if (finalSubject.isBlank()) finalSubject = suggestSubject(subject, jd, analysis);
        String body = cleanBody(response.emailBody(), fallbackName);
        String linkedIn = response.linkedinMessage() == null ? "" : response.linkedinMessage().trim();
        if (linkedIn.isBlank() || linkedIn.length() > 600) {
            linkedIn = fallbackLinkedIn(jd, fallbackName, analysis);
        }
        int ats = response.atsScore() <= 0 ? analysis.atsScore() : Math.max(0, Math.min(100, response.atsScore()));
        List<String> matched = response.matchedSkills().isEmpty() ? analysis.matchedSkills() : response.matchedSkills();
        List<String> missing = response.missingKeywords().isEmpty() ? analysis.missingKeywords() : response.missingKeywords();
        return new GenerateEmailResponse(finalSubject, body, linkedIn, ats, matched, missing);
    }

    private boolean isUsable(GenerateEmailResponse response, String resumeText) {
        if (response.emailBody() == null || response.emailBody().length() < 220) return false;
        String lower = response.emailBody().toLowerCase(Locale.ROOT);
        if (response.emailBody().contains("[") || response.emailBody().contains("]")) return false;
        if (lower.contains("hiring manager name") || lower.startsWith("dear")) return false;
        for (String skill : resumeSignals(resumeText)) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private GenerateEmailResponse fallbackResponse(String resumeText,
                                                   String jd,
                                                   String subject,
                                                   String fallbackName,
                                                   JdAnalysisResponse analysis) {
        String finalSubject = suggestSubject(subject, jd, analysis);
        String role = inferRole(finalSubject, jd);
        String years = inferExperience(resumeText);
        String skills = String.join(", ", firstN(analysis.matchedSkills().isEmpty() ? resumeSignals(resumeText) : analysis.matchedSkills(), 5));
        if (skills.isBlank()) skills = "relevant software engineering experience";
        String currentWork = currentWorkSummary(resumeText);
        String projectHook = projectHook(resumeText);

        String body = """
                Hi, I hope you're doing well.

                I am reaching out regarding %s. I have %s of experience in backend engineering with strong hands-on expertise in %s.

                In my current role, I work on %s. %s

                I have attached my resume for your consideration. I would appreciate the opportunity to discuss any suitable backend engineering openings or referral possibilities.

                Thank you for your time and consideration.

                Best regards,
                %s
                """.formatted(role, years, skills, currentWork, projectHook, fallbackName);

        return new GenerateEmailResponse(
                finalSubject,
                body,
                fallbackLinkedIn(jd, fallbackName, analysis),
                analysis.atsScore(),
                analysis.matchedSkills(),
                analysis.missingKeywords()
        );
    }

    private JdAnalysisResponse analyzeText(String resumeText, String jd) {
        List<String> resumeSignals = resumeSignals(resumeText);
        List<String> jdKeywords = jdKeywords(jd);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String resumeLower = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);
        for (String keyword : jdKeywords) {
            if (resumeLower.contains(keyword.toLowerCase(Locale.ROOT))) matched.add(keyword);
            else missing.add(keyword);
        }
        for (String signal : resumeSignals) {
            if (!matched.contains(signal)) matched.add(signal);
        }
        int score = jdKeywords.isEmpty()
                ? Math.min(75, 45 + matched.size() * 5)
                : Math.round((matched.size() * 100f) / Math.max(1, jdKeywords.size() + missing.size()));
        score = Math.max(0, Math.min(100, score));
        return new JdAnalysisResponse(score, firstN(matched, 12), firstN(missing, 12));
    }

    private List<String> resumeSignals(String resumeText) {
        List<String> signals = new ArrayList<>();
        String lower = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);
        for (String skill : KNOWN_SKILLS) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) signals.add(skill);
        }
        return signals;
    }

    private List<String> jdKeywords(String jd) {
        Set<String> keywords = new LinkedHashSet<>();
        String lower = jd == null ? "" : jd.toLowerCase(Locale.ROOT);
        for (String skill : KNOWN_SKILLS) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) keywords.add(skill);
        }
        Matcher phraseMatcher = Pattern.compile("\\b(?:java|spring|backend|microservices|distributed|aws|kafka|redis|sql|system design|rest)\\b", Pattern.CASE_INSENSITIVE).matcher(jd == null ? "" : jd);
        while (phraseMatcher.find()) keywords.add(phraseMatcher.group());
        return new ArrayList<>(keywords);
    }

    private String cleanBody(String body, String fallbackName) {
        String cleaned = body == null ? "" : body.replace("\r\n", "\n").replace("\r", "\n").trim();
        cleaned = cleaned
                .replaceAll("(?i)dear\\s+\\[[^\\]]+\\][,\\s]*", "Hi, I hope you're doing well.\n\n")
                .replaceAll("(?i)^dear\\s+(hiring manager|recruiter|referral contact)[,\\s]*", "Hi, I hope you're doing well.\n\n")
                .replaceAll("\\[[^\\]]+\\]", "")
                .trim();
        if (!cleaned.toLowerCase(Locale.ROOT).startsWith("hi,")) {
            cleaned = "Hi, I hope you're doing well.\n\n" + cleaned;
        }
        if (!cleaned.toLowerCase(Locale.ROOT).contains("best regards")) {
            cleaned += "\n\nBest regards\n" + fallbackName;
        }
        return cleaned;
    }

    private String cleanSubject(String subject) {
        if (subject == null) return "";
        String cleaned = subject.replace("\r", " ").replace("\n", " ").trim();
        return cleaned.length() > 140 ? cleaned.substring(0, 140) : cleaned;
    }

    private String suggestSubject(String subject, String jd, JdAnalysisResponse analysis) {
        String cleaned = cleanSubject(subject);
        if (!cleaned.isBlank() && !cleaned.contains("@")) return cleaned;
        String role = inferRole("", jd).replace("the ", "").replace(" role", "");
        String skills = String.join(", ", firstN(analysis.matchedSkills(), 2));
        if (skills.isBlank()) return "Application for " + role;
        return role + " | " + skills;
    }

    private String inferRole(String subject, String jd) {
        String source = !cleanSubject(subject).isBlank() ? subject : (jd == null ? "" : jd);
        String firstLine = source.split("\\n")[0].trim();
        if (firstLine.isBlank() || firstLine.contains("@")) return "this opportunity";
        String lower = firstLine.toLowerCase(Locale.ROOT);
        for (String separator : List.of("|", " - ", " at ", " role", " opportunity")) {
            int index = lower.indexOf(separator);
            if (index > 0) return "the " + firstLine.substring(0, index).trim() + " role";
        }
        return firstLine.length() > 80 ? "this opportunity" : "the " + firstLine + " role";
    }

    private String inferExperience(String resumeText) {
        Matcher matcher = YEARS_PATTERN.matcher(resumeText == null ? "" : resumeText);
        if (matcher.find()) return matcher.group(1) + "+ years";
        return "relevant";
    }

    private String fallbackName(String owner, String resumeText) {
        if (resumeText != null) {
            for (String line : resumeText.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
                String cleaned = line.trim();
                if (cleaned.matches("[A-Za-z][A-Za-z .'-]{2,60}") && cleaned.split("\\s+").length <= 4) {
                    return titleName(cleaned);
                }
            }
        }
        String local = owner == null ? "Candidate" : owner.split("@")[0].replaceAll("[._-]+", " ");
        return titleName(local);
    }

    private String titleName(String value) {
        if (value == null || value.isBlank()) return "Candidate";
        StringBuilder result = new StringBuilder();
        for (String part : value.trim().split("\\s+")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.isEmpty() ? "Candidate" : result.toString();
    }

    private String fallbackLinkedIn(String jd, String fallbackName, JdAnalysisResponse analysis) {
        String skills = String.join(", ", firstN(analysis.matchedSkills(), 3));
        if (skills.isBlank()) skills = "backend engineering";
        return "Hi, I came across this role and believe my experience in " + skills + " aligns well. I would be grateful if you could review my profile or refer me for a suitable opportunity. Thanks, " + fallbackName;
    }

    private String currentWorkSummary(String resumeText) {
        String lower = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);
        List<String> areas = new ArrayList<>();
        areas.add("backend services");
        if (lower.contains("distributed")) areas.add("distributed systems");
        areas.add("API development");
        if (lower.contains("performance")) areas.add("performance optimization");
        if (lower.contains("enterprise")) areas.add("enterprise applications");
        else areas.add("scalable backend systems");
        return String.join(", ", areas);
    }

    private String projectHook(String resumeText) {
        String lower = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);
        List<String> hooks = new ArrayList<>();
        if (lower.contains("jwt")) hooks.add("JWT authentication");
        if (lower.contains("redis") || lower.contains("cache") || lower.contains("caching")) hooks.add("caching");
        if (lower.contains("system design") || lower.contains("scalable")) hooks.add("scalable backend architectures");
        if (lower.contains("microservice")) hooks.add("microservices");
        if (hooks.isEmpty()) {
            return "I have also contributed to scalable backend architecture and production-grade API development.";
        }
        return "I have also built projects involving " + String.join(", ", firstN(hooks, 4)) + ".";
    }

    private List<String> firstN(List<String> values, int limit) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream().filter(v -> v != null && !v.isBlank()).distinct().limit(limit).toList();
    }
}
