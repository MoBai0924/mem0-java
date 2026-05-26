package com.mem0.core.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.dto.Message;
import com.mem0.core.exception.LLMException;
import com.mem0.core.llm.LLMConfig;
import com.mem0.core.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Google Gemini LLM provider implementation.
 * Uses Google AI's generateContent API to generate responses.
 *
 * @author MoBai

 */
public class GeminiLLM implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiLLM.class);

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    /**
     * Creates a new Gemini LLM provider.
     *
     * @param apiKey the API key
     */
    public GeminiLLM(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    /**
     * Creates a new Gemini LLM provider with custom settings.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL
     * @param model the model name
     */
    public GeminiLLM(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        this.objectMapper = new ObjectMapper();

        log.info("Initialized Gemini LLM provider with model: {}", this.model);
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        log.debug("Generating response for {} messages", messages.size());

        try {
            String requestBody = buildRequestBody(messages, config);

            String url = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                baseUrl, model, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                throw new LLMException("Gemini API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Failed to generate response from Gemini", e);
        }
    }

    @Override
    public String getName() {
        return "gemini";
    }

    private String buildRequestBody(List<Message> messages, LLMConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"contents\":[");

        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Message msg = messages.get(i);

            // Gemini uses "user" and "model" roles (not "assistant")
            String geminiRole = "assistant".equals(msg.role()) ? "model" : msg.role();

            sb.append("{\"role\":\"").append(escapeJson(geminiRole)).append("\",");
            sb.append("\"parts\":[{\"text\":\"").append(escapeJson(msg.content())).append("\"}]}");
        }

        sb.append("],\"generationConfig\":{");

        // Temperature
        double temperature = config.temperature() != null ? config.temperature() : 0.2;
        sb.append("\"temperature\":").append(temperature);

        // Max tokens
        if (config.maxTokens() != null) {
            sb.append(",\"maxOutputTokens\":").append(config.maxTokens());
        }

        // Top P
        if (config.topP() != null) {
            sb.append(",\"topP\":").append(config.topP());
        }

        sb.append("}}");

        return sb.toString();
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.get("candidates");

        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            throw new LLMException("Invalid response format: missing 'candidates' field");
        }

        JsonNode content = candidates.get(0).get("content");
        if (content == null) {
            throw new LLMException("Invalid response format: missing 'content' field");
        }

        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            throw new LLMException("Invalid response format: missing 'parts' field");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            JsonNode textNode = part.get("text");
            if (textNode != null) {
                text.append(textNode.asText());
            }
        }

        log.debug("Generated response of length: {}", text.length());
        return text.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
