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
 * Google Gemini embedding provider implementation.
 *
 * @author MoBai

 */
public class GeminiEmbedding implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbedding.class);

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "text-embedding-004";
    private static final int DEFAULT_DIMENSION = 768;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;

    public GeminiEmbedding(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_DIMENSION);
    }

    public GeminiEmbedding(String apiKey, String baseUrl, String model, int dimension) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();

        log.info("Initialized Gemini embedding provider with model: {}", this.model);
    }

    @Override
    public Double[] embed(String text) {
        List<Double[]> embeddings = embedBatch(List.of(text));
        if (embeddings.isEmpty()) {
            throw new EmbeddingException("Failed to generate embedding");
        }
        return embeddings.get(0);
    }

    @Override
    public List<Double[]> embedBatch(List<String> texts) {
        log.debug("Embedding {} texts via Gemini", texts.size());

        List<Double[]> results = new ArrayList<>();
        for (String text : texts) {
            try {
                String requestBody = String.format(
                        "{\"model\":\"models/%s\",\"content\":{\"parts\":[{\"text\":\"%s\"}]}}",
                        escapeJson(model), escapeJson(text)
                );

                String url = String.format("%s/v1beta/models/%s:embedContent?key=%s",
                        baseUrl, model, apiKey);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new EmbeddingException("Gemini API error: " + response.statusCode());
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode embeddingNode = root.get("embedding");
                if (embeddingNode == null) {
                    throw new EmbeddingException("Invalid Gemini response: missing 'embedding' field");
                }

                JsonNode values = embeddingNode.get("values");
                if (values == null || !values.isArray()) {
                    throw new EmbeddingException("Invalid Gemini response: missing 'values' field");
                }

                Double[] embedding = new Double[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    embedding[i] = values.get(i).asDouble();
                }
                results.add(embedding);

            } catch (EmbeddingException e) {
                throw e;
            } catch (Exception e) {
                throw new EmbeddingException("Failed to generate embedding from Gemini", e);
            }
        }
        return results;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getName() {
        return "gemini";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
