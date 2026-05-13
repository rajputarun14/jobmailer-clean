package com.arun.jobmailer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arun.jobmailer.service.S3Service;

import java.io.File;

@RestController
@RequestMapping("/admin/s3")
public class S3TestController {

    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload-test")
    public ResponseEntity<String> uploadTest(@RequestParam(required = false) String key,
                                             @RequestParam(required = false) String file) {
        String uploadKey = (key == null || key.isEmpty()) ? "Arun_Kumar_Resume.pdf" : key;
        String filePath = (file == null || file.isEmpty()) ? "resume/Arun_Kumar_Resume.pdf" : file;
        File f = new File(filePath);
        if (!f.exists()) return ResponseEntity.badRequest().body("File not found: " + filePath);
        try {
            s3Service.uploadFile(uploadKey, f);
            return ResponseEntity.ok("Uploaded to S3: " + uploadKey);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}
