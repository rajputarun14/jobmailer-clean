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
import com.arun.jobmailer.dto.GenerateEmailResponse;


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
        String prompt = ResumeTailorPrompts.geminiFullPrompt(resumeText, jdText);
        return generateText(prompt, 8192, 0.25);
    }

    @Override
    public String draftApplicationEmail(String resumeText, String jdText, String subject, String fallbackName) throws Exception {
        String prompt = ResumeTailorPrompts.emailDraftPrompt(resumeText, jdText, subject, fallbackName);
        return generateText(prompt, 1200, 0.35);
    }

    @Override
    public GenerateEmailResponse generateApplicationEmail(String resumeText, String jdText, String subject, String fallbackName) throws Exception {
        String prompt = ResumeTailorPrompts.structuredEmailPrompt(resumeText, jdText, subject, fallbackName);
        String json = stripJsonFence(generateText(prompt, 1800, 0.25));
        return mapper.readValue(json, GenerateEmailResponse.class);
    }

    private String generateText(String prompt, int maxOutputTokens, double temperature) throws Exception {
        String apiKey = appSettingService.getGeminiApiKey()
                .orElse(fallbackApiKey == null ? "" : fallbackApiKey);
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Gemini API key not configured");

        String url = String.format(apiUrlTemplate, model);

        Map<String, Object> body = Map.of(
            "contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", prompt)
                })
            },
            "generationConfig", Map.of(
                "maxOutputTokens", maxOutputTokens,
                "temperature", temperature
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

    private String stripJsonFence(String value) {
        if (value == null) return "{}";
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("(?s)^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("(?s)\\s*```$", "");
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return trimmed.substring(first, last + 1);
        }
        return trimmed;
    }
}
