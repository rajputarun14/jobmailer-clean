package com.arun.jobmailer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private final Path uploadsRoot;
    private static final String CURRENT_FILE = "current.txt";

    public UploadService(@org.springframework.beans.factory.annotation.Value("${uploads.root:uploads}") String uploadsRootStr) throws IOException {
        this.uploadsRoot = Paths.get(uploadsRootStr);
        Files.createDirectories(this.uploadsRoot);
    }

    /**
     * Save uploaded resume for the given owner. Accepts only PDF files.
     * Returns the generated id (filename without extension).
     */
    public String saveResume(MultipartFile file, String owner) throws IOException {
        if (file == null || file.isEmpty()) throw new IOException("Empty file");
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".pdf")) {
            throw new IOException("Only PDF resumes are allowed");
        }

        String id = UUID.randomUUID().toString();
        Path ownerDir = uploadsRoot.resolve(safeOwner(owner));
        Files.createDirectories(ownerDir);
        // delete previous current resume if present
        String prev = getCurrentId(owner);
        Path dest = ownerDir.resolve(id + ".pdf");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        if (prev != null && !prev.isBlank()) {
            Path prevPath = ownerDir.resolve(prev + ".pdf");
            try { Files.deleteIfExists(prevPath); } catch (Exception ignore) {}
        }
        // write current marker with original filename
        String original1 = file.getOriginalFilename();
        if (original1 == null) original1 = id + ".pdf";
        Files.writeString(ownerDir.resolve(CURRENT_FILE), id + "::" + original1);
        return id;
    }

    public Path getResumePath(String owner, String id) {
        Path ownerDir = uploadsRoot.resolve(safeOwner(owner));
        return ownerDir.resolve(id + ".pdf");
    }

    /** Returns the id of the current resume for owner, or null. */
    public String getCurrentId(String owner) {
        try {
            Path ownerDir = uploadsRoot.resolve(safeOwner(owner));
            Path cur = ownerDir.resolve(CURRENT_FILE);
            if (Files.exists(cur)) {
                String s = Files.readString(cur).trim();
                String[] parts = s.split("::", 2);
                return parts.length > 0 ? parts[0] : null;
            }
        } catch (Exception ignore) {}
        return null;
    }

    /** Returns the original filename of the current resume for owner, or null. */
    public String getCurrentOriginalName(String owner) {
        try {
            Path ownerDir = uploadsRoot.resolve(safeOwner(owner));
            Path cur = ownerDir.resolve(CURRENT_FILE);
            if (Files.exists(cur)) {
                String s = Files.readString(cur).trim();
                String[] parts = s.split("::", 2);
                return parts.length > 1 ? parts[1] : null;
            }
        } catch (Exception ignore) {}
        return null;
    }

    /** Returns path to the current resume file for owner, or null if none. */
    public Path getCurrentResumePath(String owner) {
        String id = getCurrentId(owner);
        if (id == null) return null;
        return getResumePath(owner, id);
    }

    private String safeOwner(String owner) {
        if (owner == null || owner.isBlank()) return "anonymous";
        return owner.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
