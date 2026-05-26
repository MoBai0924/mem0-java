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
 * Ollama LLM provider implementation.
 * Uses Ollama's chat API for local LLM models.
 *
 * @author MoBai

 */
public class OllamaLLM implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaLLM.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;

    /**
     * Creates a new Ollama LLM provider with default settings.
     */
    public OllamaLLM() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    /**
     * Creates a new Ollama LLM provider.
     *
     * @param baseUrl the base URL
     * @param model the model name
     */
    public OllamaLLM(String baseUrl, String model) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        this.objectMapper = new ObjectMapper();

        log.info("Initialized Ollama LLM provider with model: {}", this.model);
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        log.debug("Generating response for {} messages", messages.size());

        try {
            String requestBody = buildRequestBody(messages, config);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(300))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama API error: {} - {}", response.statusCode(), response.body());
                throw new LLMException("Ollama API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Failed to generate response from Ollama", e);
        }
    }

    @Override
    public String getName() {
        return "ollama";
    }

    private String buildRequestBody(List<Message> messages, LLMConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(escapeJson(model)).append("\",");
        sb.append("\"stream\":false,");

        // Options
        sb.append("\"options\":{");
        double temperature = config.temperature() != null ? config.temperature() : 0.2;
        sb.append("\"temperature\":").append(temperature);

        if (config.topP() != null) {
            sb.append(",\"top_p\":").append(config.topP());
        }

        sb.append("},");

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
        JsonNode message = root.get("message");

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
