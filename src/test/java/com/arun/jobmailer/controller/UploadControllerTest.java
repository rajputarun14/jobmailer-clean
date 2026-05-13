package com.arun.jobmailer.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import com.arun.jobmailer.service.UploadService;

class UploadControllerTest {

    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("unchecked")
    void currentResumeReturnsEmptyStateWhenUserHasNotUploadedResume() throws Exception {
        UploadController controller = new UploadController(new UploadService(tempDir.toString()));

        ResponseEntity<Object> response = controller.currentResume(() -> "alice@example.com");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("hasResume", false);
        assertThat(body).containsEntry("id", "");
        assertThat(body).containsEntry("name", "");
    }
}
