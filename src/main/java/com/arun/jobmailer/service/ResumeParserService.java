package com.arun.jobmailer.service;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

@Service
public class ResumeParserService {

    private final PdfResumeService pdfResumeService;

    public ResumeParserService(PdfResumeService pdfResumeService) {
        this.pdfResumeService = pdfResumeService;
    }

    public String extractText(Path resumePath) throws IOException {
        if (resumePath == null) {
            throw new IOException("Resume path is required");
        }
        String fileName = resumePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return pdfResumeService.extractText(resumePath);
        }
        if (fileName.endsWith(".docx")) {
            return extractDocxText(resumePath);
        }
        throw new IOException("Unsupported resume format. Upload PDF or DOCX");
    }

    private String extractDocxText(Path resumePath) throws IOException {
        try (XWPFDocument document = new XWPFDocument(java.nio.file.Files.newInputStream(resumePath))) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String value = paragraph.getText();
                if (value != null && !value.isBlank()) {
                    if (!text.isEmpty()) text.append('\n');
                    text.append(value.trim());
                }
            }
            return text.toString();
        }
    }
}
