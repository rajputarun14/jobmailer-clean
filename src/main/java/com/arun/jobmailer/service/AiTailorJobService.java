package com.arun.jobmailer.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.arun.jobmailer.ai.AIDraftService;

@Service
public class AiTailorJobService {

    private final AIDraftService aiDraftService;
    private final UploadService uploadService;
    private final PdfResumeService pdfResumeService;
    private final Executor aiTaskExecutor;
    private final Map<String, AiTailorJob> jobs = new ConcurrentHashMap<>();

    public AiTailorJobService(AIDraftService aiDraftService,
                              UploadService uploadService,
                              PdfResumeService pdfResumeService,
                              @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.aiDraftService = aiDraftService;
        this.uploadService = uploadService;
        this.pdfResumeService = pdfResumeService;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    public AiTailorJob submit(String owner, String resumeId, String jd) {
        String jobId = UUID.randomUUID().toString();
        AiTailorJob job = new AiTailorJob(jobId, owner);
        jobs.put(jobId, job);
        aiTaskExecutor.execute(() -> run(job, resumeId, jd));
        return job;
    }

    public AiTailorJob getForOwner(String jobId, String owner) {
        AiTailorJob job = jobs.get(jobId);
        if (job == null || !job.getOwner().equals(owner)) return null;
        return job;
    }

    void run(AiTailorJob job, String resumeId, String jd) {
        job.setStatus(AiTailorJob.Status.RUNNING);
        try {
            Path resumePath = (resumeId == null || resumeId.isBlank())
                    ? uploadService.getCurrentResumePath(job.getOwner())
                    : uploadService.getResumePath(job.getOwner(), resumeId);
            if (resumePath == null || !Files.exists(resumePath)) {
                throw new IllegalArgumentException("resume not found");
            }

            String resumeText = pdfResumeService.extractText(resumePath);
            String tailored = aiDraftService.tailorResume(resumeText, jd == null ? "" : jd);
            tailored = stripCodeFences(tailored);

            String tailoredId = UUID.randomUUID().toString();
            Path tailoredDir = uploadService.getTailoredDir(job.getOwner());
            Files.createDirectories(tailoredDir);
            Path outputPath = tailoredDir.resolve(tailoredId + ".pdf");
            pdfResumeService.writeTextPdf(tailored, outputPath);
            job.setInfo("Single tailored PDF: wording mirrors your resume structure and the JD. Typography is a simple export (standard font), not your original PDF design.");
            job.setTailoredId(tailoredId);
            job.setDownloadUrl("/downloadTailored?id=" + tailoredId);
            job.setStatus(AiTailorJob.Status.COMPLETED);
        } catch (Exception e) {
            job.setError(e.getMessage());
            job.setStatus(AiTailorJob.Status.FAILED);
        }
    }

    private static final Pattern MARKDOWN_FENCE_LANG = Pattern.compile("(?i)(text|plaintext|markdown|md|txt)");

    /**
     * Models sometimes wrap output in Markdown fences; strip so PDF text stays clean.
     */
    static String stripCodeFences(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        if (!t.startsWith("```")) {
            return t;
        }
        t = t.substring(3).stripLeading();
        int nl = t.indexOf('\n');
        if (nl > 0) {
            String first = t.substring(0, nl).trim();
            if (first.length() <= 24 && MARKDOWN_FENCE_LANG.matcher(first).matches()) {
                t = t.substring(nl + 1);
            }
        }
        t = t.strip();
        if (t.endsWith("```")) {
            t = t.substring(0, t.length() - 3).strip();
        }
        return t.strip();
    }
}
