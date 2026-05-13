package com.arun.jobmailer.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfResumeService {

    private static final float FONT_SIZE = 10;
    private static final float LEADING = 13;
    private static final float MARGIN = 50;

    public String extractText(Path pdfPath) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    public void writeTextPdf(String text, Path outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDPageContentStream stream = startPage(document, page);
            float y = page.getMediaBox().getHeight() - MARGIN;
            float width = page.getMediaBox().getWidth() - (2 * MARGIN);

            for (String paragraph : text.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
                List<String> lines = wrap(paragraph, width);
                if (lines.isEmpty()) lines = List.of("");

                for (String line : lines) {
                    if (y < MARGIN) {
                        stream.endText();
                        stream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        stream = startPage(document, page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                    }
                    stream.showText(sanitize(line));
                    stream.newLineAtOffset(0, -LEADING);
                    y -= LEADING;
                }
            }

            stream.endText();
            stream.close();
            document.save(outputPath.toFile());
        }
    }

    private PDPageContentStream startPage(PDDocument document, PDPage page) throws IOException {
        PDPageContentStream stream = new PDPageContentStream(document, page);
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
        stream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN);
        return stream;
    }

    private List<String> wrap(String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String normalized = sanitize(text).trim();
        if (normalized.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (width(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private float width(String value) throws IOException {
        return PDType1Font.HELVETICA.getStringWidth(value) / 1000 * FONT_SIZE;
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");
    }
}
