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
import com.arun.jobmailer.service.AppSettingService;


@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "gemini")
public class GeminiAIDraftService implements AIDraftService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String fallbackApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent}")
    private String apiUrlTemplate;

    private final AppSettingService appSettingService;

    public GeminiAIDraftService(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @Override
    public String tailorResume(String resumeText, String jdText) throws Exception {
        String apiKey = appSettingService.getGeminiApiKey()
                .orElse(fallbackApiKey == null ? "" : fallbackApiKey);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Gemini API key not configured");

        String url = String.format(apiUrlTemplate, model);

        String prompt = ResumeTailorPrompts.geminiFullPrompt(resumeText, jdText);

        // Correct JSON structure for Gemini API
        Map<String, Object> body = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", prompt)
                })
            },
            "generationConfig", Map.of(
                "maxOutputTokens", 8192,
                "temperature", 0.25
            )
        );
        
        String bodyJson = mapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
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
