package com.arun.jobmailer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsPathTraversalForResumeIds() throws Exception {
        UploadService service = new UploadService(tempDir.toString());

        assertThatThrownBy(() -> service.getResumePath("alice", "../secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid resume id");
    }

    @Test
    void tailoredResumePathStaysInsideOwnerDirectory() throws Exception {
        UploadService service = new UploadService(tempDir.toString());
        String id = UUID.randomUUID().toString();

        Path path = service.getTailoredResumePath("alice@example.com", id);

        assertThat(path.normalize().toString()).startsWith(service.getTailoredDir("alice@example.com").normalize().toString());
        assertThat(path.getFileName().toString()).isEqualTo(id + ".pdf");
    }
}
