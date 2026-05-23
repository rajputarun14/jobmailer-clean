package com.arun.jobmailer.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arun.jobmailer.dto.AsyncEmailJobResponse;
import com.arun.jobmailer.dto.AsyncEmailJobStatusResponse;
import com.arun.jobmailer.dto.GenerateEmailRequest;
import com.arun.jobmailer.service.AiTailorJob;
import com.arun.jobmailer.service.AiTailorJobService;
import com.arun.jobmailer.service.AsyncEmailJob;
import com.arun.jobmailer.service.AiEmailGenerationService;

import java.security.Principal;

@RestController
public class AIController {

    @Autowired
    private AiTailorJobService aiTailorJobService;

    @Autowired
    private AiEmailGenerationService aiEmailGenerationService;

    @PostMapping("/ai/tailorResume")
    public ResponseEntity<Object> tailorResume(@RequestParam(required=false) String resumeId,
                                               @RequestParam String jd,
                                               Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            AiTailorJob job = aiTailorJobService.submit(owner, resumeId, jd);
            return ResponseEntity.accepted().body(Map.of(
                    "jobId", job.getId(),
                    "status", job.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/ai/tailorResume/{jobId}")
    public ResponseEntity<Object> getTailorJob(@PathVariable String jobId, Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        AiTailorJob job = aiTailorJobService.getForOwner(jobId, owner);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "job not found"));
        }
        return ResponseEntity.ok(Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "tailoredId", job.getTailoredId() == null ? "" : job.getTailoredId(),
                "downloadUrl", job.getDownloadUrl() == null ? "" : job.getDownloadUrl(),
                "error", job.getError() == null ? "" : job.getError(),
                "info", job.getInfo() == null ? "" : job.getInfo()
        ));
    }

    @PostMapping("/api/ai/generate-email")
    public ResponseEntity<Object> generateEmail(@RequestBody GenerateEmailRequest request,
                                                Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            if (request == null || request.jd() == null || request.jd().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "JD is required"));
            }
            return ResponseEntity.ok(aiEmailGenerationService.generate(owner, request.resumeId(), request.jd(), request.subject()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/ai/generate-email/async")
    public ResponseEntity<Object> generateEmailAsync(@RequestBody GenerateEmailRequest request,
                                                     Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        if (request == null || request.jd() == null || request.jd().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "JD is required"));
        }
        AsyncEmailJob job = aiEmailGenerationService.submit(owner, request.resumeId(), request.jd(), request.subject());
        return ResponseEntity.accepted().body(new AsyncEmailJobResponse(job.getId(), job.getStatus()));
    }

    @GetMapping("/api/ai/generate-email/{jobId}")
    public ResponseEntity<Object> getGeneratedEmail(@PathVariable String jobId, Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        AsyncEmailJob job = aiEmailGenerationService.getForOwner(jobId, owner);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "job not found"));
        }
        return ResponseEntity.ok(new AsyncEmailJobStatusResponse(job.getId(), job.getStatus(), job.getResult(), job.getError() == null ? "" : job.getError()));
    }

    @PostMapping("/api/ai/analyze-jd")
    public ResponseEntity<Object> analyzeJd(@RequestBody GenerateEmailRequest request,
                                            Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            if (request == null || request.jd() == null || request.jd().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "JD is required"));
            }
            return ResponseEntity.ok(aiEmailGenerationService.analyze(owner, request.resumeId(), request.jd()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
