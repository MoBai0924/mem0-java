package com.mem0.core.embedding.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.embedding.EmbeddingProvider;
import com.mem0.core.exception.EmbeddingException;
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
 * OpenAI embedding provider implementation.
 * Uses OpenAI's embedding API to generate embeddings.
 *
 * @author MoBai

 */
public class OpenAIEmbedding implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbedding.class);

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "text-embedding-3-small";
    private static final int DEFAULT_DIMENSION = 1536;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;

    /**
     * Creates a new OpenAI embedding provider.
     *
     * @param apiKey the API key
     */
    public OpenAIEmbedding(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_DIMENSION);
    }

    /**
     * Creates a new OpenAI embedding provider with custom settings.
     *
     * @param apiKey    the API key
     * @param baseUrl   the base URL
     * @param model     the model name
     * @param dimension the embedding dimension
     */
    public OpenAIEmbedding(String apiKey, String baseUrl, String model, int dimension) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.objectMapper = new ObjectMapper();

        log.info("Initialized OpenAI embedding provider with model: {} and dimension: {}", this.model, this.dimension);
    }

    @Override
    public Double[] embed(String text) {
        log.debug("Embedding text of length: {}", text.length());

        List<Double[]> embeddings = embedBatch(List.of(text));
        if (embeddings.isEmpty()) {
            throw new EmbeddingException("Failed to generate embedding");
        }

        return embeddings.get(0);
    }

    @Override
    public List<Double[]> embedBatch(List<String> texts) {
        log.debug("Embedding {} texts", texts.size());

        try {
            // Build request body
            String requestBody = buildRequestBody(texts);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response
            if (response.statusCode() != 200) {
                log.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
                throw new EmbeddingException("OpenAI API error: " + response.statusCode());
            }

            return parseEmbeddings(response.body());

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate embeddings", e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getName() {
        return "openai";
    }

    // Helper methods

    private String buildRequestBody(List<String> texts) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",");
        sb.append("\"input\":[");

        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJson(texts.get(i))).append("\"");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private List<Double[]> parseEmbeddings(String responseBody) throws Exception {
        List<Double[]> embeddings = new ArrayList<>();

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray()) {
            throw new EmbeddingException("Invalid response format: missing 'data' field");
        }

        for (JsonNode item : data) {
            JsonNode embeddingArray = item.get("embedding");
            if (embeddingArray == null || !embeddingArray.isArray()) {
                throw new EmbeddingException("Invalid response format: missing 'embedding' field");
            }

            Double[] embedding = new Double[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).asDouble();
            }

            embeddings.add(embedding);
        }

        log.debug("Parsed {} embeddings", embeddings.size());
        return embeddings;
    }
}
