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
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai")
public class OpenAIAIDraftService implements AIDraftService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Override
    public String tailorResume(String resumeText, String jdText) throws Exception {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OpenAI API key not configured");

        String system = ResumeTailorPrompts.INSTRUCTIONS;
        String user = ResumeTailorPrompts.openAiUserContent(resumeText, jdText);

        Map<String,Object> requestBody = Map.of(
            "model", model,
            "messages", new Object[] {
                Map.of("role","system","content",system),
                Map.of("role","user","content",user)
            },
            "max_tokens", 8192,
            "temperature", 0.25
        );

        String body = mapper.writeValueAsString(requestBody);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type","application/json")
            .header("Authorization","Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("AI provider error: " + resp.statusCode() + " " + resp.body());
        }

        JsonNode root = mapper.readTree(resp.body());
        // chat completions: choices[0].message.content
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) throw new IllegalStateException("No choices from AI");
        JsonNode first = choices.get(0);
        String text = first.path("message").path("content").asText();
        if (text == null || text.isBlank()) text = first.path("text").asText();
        return text;
    }
}
