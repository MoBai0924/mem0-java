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
import java.util.List;

/**
 * Azure OpenAI LLM provider implementation.
 * Uses Azure OpenAI's chat completions API to generate responses.
 *
 * @author MoBai

 */
public class AzureOpenAILLM implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAILLM.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String endpoint;
    private final String deploymentName;
    private final String apiVersion;

    /**
     * Creates a new Azure OpenAI LLM provider.
     *
     * @param apiKey the API key
     * @param endpoint the Azure endpoint (e.g., "https://resource.openai.azure.com")
     * @param deploymentName the deployment name
     */
    public AzureOpenAILLM(String apiKey, String endpoint, String deploymentName) {
        this(apiKey, endpoint, deploymentName, "2024-02-01");
    }

    /**
     * Creates a new Azure OpenAI LLM provider with custom API version.
     *
     * @param apiKey the API key
     * @param endpoint the Azure endpoint
     * @param deploymentName the deployment name
     * @param apiVersion the API version
     */
    public AzureOpenAILLM(String apiKey, String endpoint, String deploymentName, String apiVersion) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.deploymentName = deploymentName;
        this.apiVersion = apiVersion != null ? apiVersion : "2024-02-01";

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        this.objectMapper = new ObjectMapper();

        log.info("Initialized Azure OpenAI LLM provider with deployment: {}", this.deploymentName);
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        log.debug("Generating response for {} messages", messages.size());

        try {
            String requestBody = buildRequestBody(messages, config);

            String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                endpoint, deploymentName, apiVersion);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Azure OpenAI API error: {} - {}", response.statusCode(), response.body());
                throw new LLMException("Azure OpenAI API error: " + response.statusCode() + " - " + response.body());
            }

            return parseResponse(response.body());

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("Failed to generate response from Azure OpenAI", e);
        }
    }

    @Override
    public String getName() {
        return "azure";
    }

    private String buildRequestBody(List<Message> messages, LLMConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

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
