package com.arun.jobmailer.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.arun.jobmailer.dto.ResumeUploadResponse;
import com.arun.jobmailer.service.UploadService;

@RestController
public class UploadController {

    @Autowired
    private UploadService uploadService;

    public UploadController() {}

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/uploadResume")
    public ResponseEntity<Object> uploadResume(@RequestParam("file") MultipartFile file, Principal principal) {
        return uploadResumeDocument(file, principal);
    }

    @PostMapping("/api/resume/upload")
    public ResponseEntity<Object> uploadResumeDocument(@RequestParam("file") MultipartFile file, Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            String id = uploadService.saveResume(file, owner);
            String name = uploadService.getCurrentOriginalName(owner);
            return ResponseEntity.ok(new ResumeUploadResponse(true, id, name == null ? "" : name, uploadService.getCurrentFileType(owner)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/currentResume")
    public ResponseEntity<Object> currentResume(Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            String id = uploadService.getCurrentId(owner);
            String name = uploadService.getCurrentOriginalName(owner);
            return ResponseEntity.ok(resumeResponse(owner, id, name));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/downloadResume")
    public ResponseEntity<Resource> downloadResume(Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            java.nio.file.Path p = uploadService.getCurrentResumePath(owner);
            if (p == null || !java.nio.file.Files.exists(p)) {
                return ResponseEntity.notFound().build();
            }
            Resource res = new PathResource(p);
                String original = uploadService.getCurrentOriginalName(owner);
                String fileName = original != null ? original : p.getFileName().toString();
                MediaType contentType = fileName.toLowerCase().endsWith(".docx")
                        ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        : MediaType.APPLICATION_PDF;
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(contentType)
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/downloadTailored")
    public ResponseEntity<Resource> downloadTailored(@RequestParam String id, Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            java.nio.file.Path p = uploadService.getTailoredResumePath(owner, id);
            if (!java.nio.file.Files.exists(p)) return ResponseEntity.notFound().build();
            Resource res = new PathResource(p);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + p.getFileName().toString() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> resumeResponse(String owner, String id, String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("hasResume", id != null && !id.isBlank());
        response.put("id", id == null ? "" : id);
        response.put("name", name == null ? "" : name);
        response.put("fileType", id == null || id.isBlank() ? "" : uploadService.getCurrentFileType(owner));
        return response;
    }
}
