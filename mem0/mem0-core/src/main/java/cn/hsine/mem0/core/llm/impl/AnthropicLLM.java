package cn.hsine.mem0.core.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.dto.Message;
import cn.hsine.mem0.core.exception.LLMException;
import cn.hsine.mem0.core.llm.LLMConfig;
import cn.hsine.mem0.core.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic LLM provider implementation.
 * Uses Anthropic's messages API to generate responses.
 *
 * @author MoBai

 */
public class AnthropicLLM implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLLM.class);

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL = "claude-3-5-sonnet-20241022";
    private static final String API_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    /**
     * Creates a new Anthropic LLM provider.
     *
     * @param apiKey the API key
     */
    public AnthropicLLM(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    /**
     * Creates a new Anthropic LLM provider with custom settings.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL
     * @param model the model name
     */
    public AnthropicLLM(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        this.objectMapper = new ObjectMapper();

        log.info("Initialized Anthropic LLM provider with model: {}", this.model);
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        log.debug("Generating response for {} messages", messages.size());

        try {
            // Anthropic requires system message to be separate from messages
            String systemPrompt = extractSystemPrompt(messages);
            List<Message> nonSystemMessages = filterNonSystemMessages(messages);

            String requestBody = buildRequestBody(nonSystemMessages, systemPrompt, config);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Anthropic API error: {} - {}", response.statusCode(), response.body());
                throw new LLMException("Anthropic API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Failed to generate response from Anthropic", e);
        }
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    private String extractSystemPrompt(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            if ("system".equals(msg.role())) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(msg.content());
            }
        }
        return sb.toString();
    }

    private List<Message> filterNonSystemMessages(List<Message> messages) {
        List<Message> filtered = new ArrayList<>();
        for (Message msg : messages) {
            if (!"system".equals(msg.role())) {
                filtered.add(msg);
            }
        }
        return filtered;
    }

    private String buildRequestBody(List<Message> messages, String systemPrompt, LLMConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(escapeJson(model)).append("\",");

        // System prompt (Anthropic-specific: separate from messages)
        if (!systemPrompt.isEmpty()) {
            sb.append("\"system\":\"").append(escapeJson(systemPrompt)).append("\",");
        }

        // Max tokens (required by Anthropic)
        int maxTokens = config.maxTokens() != null ? config.maxTokens() : 1500;
        sb.append("\"max_tokens\":").append(maxTokens).append(",");

        // Temperature
        double temperature = config.temperature() != null ? config.temperature() : 0.2;
        sb.append("\"temperature\":").append(temperature).append(",");

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
        JsonNode content = root.get("content");

        if (content == null || !content.isArray() || content.isEmpty()) {
            throw new LLMException("Invalid response format: missing 'content' field");
        }

        // Anthropic returns content as array of blocks: [{"type":"text","text":"..."}]
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.get("type").asText())) {
                text.append(block.get("text").asText());
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
