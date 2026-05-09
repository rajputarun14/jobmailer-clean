package com.arun.jobmailer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arun.jobmailer.ai.AIDraftService;
import com.arun.jobmailer.service.UploadService;

import java.security.Principal;

@RestController
public class AIController {

    @Autowired
    private AIDraftService ai;

    @Autowired
    private UploadService uploadService;

    @PostMapping("/ai/tailorResume")
    public ResponseEntity<Object> tailorResume(@RequestParam(required=false) String resumeId,
                                               @RequestParam String jd,
                                               Principal principal) {
        String owner = principal == null ? "anonymous" : principal.getName();
        try {
            Path resumePath;
            if (resumeId == null || resumeId.isBlank()) {
                resumePath = uploadService.getCurrentResumePath(owner);
            } else {
                resumePath = uploadService.getResumePath(owner, resumeId);
            }
            if (resumePath == null || !Files.exists(resumePath)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error","resume not found"));

            // extract text
            String resumeText;
            try (PDDocument doc = PDDocument.load(resumePath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                resumeText = stripper.getText(doc);
            }

            String tailored = ai.tailorResume(resumeText, jd == null ? "" : jd);

            // render tailored text to PDF
            String tid = UUID.randomUUID().toString();
            Path tailoredDir = Paths.get(uploadService.getResumePath(owner, "").getParent().toString(), "tailored");
            Files.createDirectories(tailoredDir);
            Path out = tailoredDir.resolve(tid + ".pdf");

            try (PDDocument pdoc = new PDDocument()) {
                PDPage page = new PDPage();
                pdoc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(pdoc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(50, 750);
                    String[] lines = tailored.split("\r?\n");
                    int yOffset = 0;
                    for (String line : lines) {
                        // simple wrapping
                        String toWrite = line;
                        if (toWrite.length() > 100) toWrite = toWrite.substring(0, 100);
                        cs.showText(toWrite);
                        cs.newLineAtOffset(0, -12);
                        yOffset += 12;
                        if (yOffset > 700) break;
                    }
                    cs.endText();
                }
                pdoc.save(out.toFile());
            }

            String downloadUrl = "/downloadTailored?owner=" + owner + "&id=" + tid;
            return ResponseEntity.ok(Map.of("tailoredId", tid, "downloadUrl", downloadUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
