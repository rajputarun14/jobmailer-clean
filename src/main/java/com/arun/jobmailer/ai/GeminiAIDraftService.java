package com.arun.jobmailer.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "gemini")
public class GeminiAIDraftService implements AIDraftService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.0}")
    private String model;

    @Value("${gemini.api.url:https://api.generativemodels.googleapis.com/v1/models/%s:generate}")
    private String apiUrlTemplate;


    @Override
    public String tailorResume(String resumeText, String jdText) throws Exception {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Gemini API key not configured");

        // Gemini typically takes the key as a query param: ?key=YOUR_KEY
        String url = String.format(apiUrlTemplate, model) + "?key=" + apiKey;

        String prompt = "You are a resume assistant. Produce an ATS-friendly plain-text resume tailored to the provided Job Description. "
                + "Preserve factual information (company names, job titles, dates) and only rephrase or reorder bullets. Do NOT invent jobs or dates. "
                + "Output the tailored resume in plain text with clear section headings (Summary, Experience, Education, Skills).\n\n"
                + "Resume:\n" + resumeText + "\n\nJob Description:\n" + (jdText == null ? "" : jdText) + "\n\nReturn only the tailored resume text.";

        // Correct JSON structure for Gemini API
        Map<String, Object> body = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", prompt)
                })
            },
            "generationConfig", Map.of(
                "maxOutputTokens", 2048,
                "temperature", 0.7
            )
        );
        
        String bodyJson = mapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Gemini API error: " + resp.statusCode() + " " + resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        
        // Navigation: candidates[0] -> content -> parts[0] -> text
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText();
            }
        }
        
        throw new RuntimeException("Failed to extract text from Gemini response: " + resp.body());
    }

}
