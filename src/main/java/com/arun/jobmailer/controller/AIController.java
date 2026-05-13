package com.arun.jobmailer.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arun.jobmailer.service.AiTailorJob;
import com.arun.jobmailer.service.AiTailorJobService;

import java.security.Principal;

@RestController
public class AIController {

    @Autowired
    private AiTailorJobService aiTailorJobService;

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
}
