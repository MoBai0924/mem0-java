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
 * OpenAI LLM provider implementation.
 * Uses OpenAI's chat completions API to generate responses.
 *
 * @author MoBai

 */
public class OpenAILLM implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAILLM.class);

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    /**
     * Creates a new OpenAI LLM provider.
     *
     * @param apiKey the API key
     */
    public OpenAILLM(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    /**
     * Creates a new OpenAI LLM provider with custom settings.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL
     * @param model the model name
     */
    public OpenAILLM(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        this.objectMapper = new ObjectMapper();

        log.info("Initialized OpenAI LLM provider with model: {}", this.model);
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        log.debug("Generating response for {} messages", messages.size());

        try {
            String requestBody = buildRequestBody(messages, config);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
                throw new LLMException("OpenAI API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Failed to generate response from OpenAI", e);
        }
    }

    @Override
    public String getName() {
        return "openai";
    }

    private String buildRequestBody(List<Message> messages, LLMConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(escapeJson(model)).append("\",");

        // Temperature
        double temperature = config.temperature() != null ? config.temperature() : 0.2;
        sb.append("\"temperature\":").append(temperature).append(",");

        // Max tokens
        if (config.maxTokens() != null) {
            sb.append("\"max_tokens\":").append(config.maxTokens()).append(",");
        }

        // Top P
        if (config.topP() != null) {
            sb.append("\"top_p\":").append(config.topP()).append(",");
        }

        // Messages
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            Message msg = messages.get(i);
            sb.append("{\"role\":\"").append(escapeJson(msg.role())).append("\",");
            sb.append("\"content\":\"").append(escapeJson(msg.content())).append("\"}");
        }
        sb.append("]}");

        return sb.toString();
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");

        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new LLMException("Invalid response format: missing 'choices' field");
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null) {
            throw new LLMException("Invalid response format: missing 'message' field");
        }

        JsonNode content = message.get("content");
        if (content == null) {
            throw new LLMException("Invalid response format: missing 'content' field");
        }

        String text = content.asText();
        log.debug("Generated response of length: {}", text.length());
        return text;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
