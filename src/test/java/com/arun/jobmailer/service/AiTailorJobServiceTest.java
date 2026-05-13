package com.arun.jobmailer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.arun.jobmailer.ai.AIDraftService;

class AiTailorJobServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void stripCodeFences_removesKnownMarkdownFence() {
        assertThat(AiTailorJobService.stripCodeFences("```text\nHello\nWorld\n```")).isEqualTo("Hello\nWorld");
    }

    @Test
    void stripCodeFences_doesNotStripFirstLineOfResume() {
        String body = "SUMMARY\nExperienced engineer";
        assertThat(AiTailorJobService.stripCodeFences("```\n" + body + "\n```")).isEqualTo(body);
    }

    @Test
    void completesTailoringJobAndWritesPdf() throws Exception {
        UploadService uploadService = new UploadService(tempDir.toString());
        PdfResumeService pdfResumeService = new PdfResumeService();
        AIDraftService ai = (resumeText, jdText) -> "Summary\nTailored for " + jdText + "\nExperience\n" + resumeText;
        AiTailorJobService jobService = new AiTailorJobService(ai, uploadService, pdfResumeService, Runnable::run);

        String owner = "alice";
        String resumeId = UUID.randomUUID().toString();
        Path resumePath = uploadService.getResumePath(owner, resumeId);
        Files.createDirectories(resumePath.getParent());
        pdfResumeService.writeTextPdf("Original Java resume", resumePath);

        AiTailorJob job = jobService.submit(owner, resumeId, "Spring Boot");

        assertThat(job.getStatus()).isEqualTo(AiTailorJob.Status.COMPLETED);
        assertThat(job.getTailoredId()).isNotBlank();
        assertThat(Files.exists(uploadService.getTailoredResumePath(owner, job.getTailoredId()))).isTrue();
    }

    @Test
    void keepsJobsScopedToOwner() throws Exception {
        UploadService uploadService = new UploadService(tempDir.toString());
        PdfResumeService pdfResumeService = new PdfResumeService();
        AiTailorJobService jobService = new AiTailorJobService((resume, jd) -> "tailored", uploadService, pdfResumeService, Runnable::run);

        AiTailorJob job = jobService.submit("alice", null, "jd");

        assertThat(jobService.getForOwner(job.getId(), "bob")).isNull();
    }
}
