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
 * Google Vertex AI embedding provider implementation.
 */
public class VertexAIEmbedding implements EmbeddingProvider {
    private static final Logger log = LoggerFactory.getLogger(VertexAIEmbedding.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final String projectId;
    private final String location;
    private final String model;
    private final int dimension;

    public VertexAIEmbedding(String accessToken, String projectId, String location, String model, int dimension) {
        this.accessToken = accessToken;
        this.projectId = projectId;
        this.location = location != null ? location : "us-central1";
        this.model = model != null ? model : "text-embedding-004";
        this.dimension = dimension > 0 ? dimension : 768;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
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
        List<Double[]> results = new ArrayList<>();
        for (String text : texts) {
            try {
                String requestBody = String.format("{\"instances\":[{\"content\":\"%s\"}]}", escapeJson(text));
                String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
                        location, projectId, location, model);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(60)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new EmbeddingException("VertexAI API error: " + response.statusCode());
                }
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode embeddingsNode = root.at("/predictions/0/embeddings");
                if (embeddingsNode == null || !embeddingsNode.isArray()) {
                    throw new EmbeddingException("Invalid VertexAI response");
                }
                Double[] embedding = new Double[embeddingsNode.size()];
                for (int i = 0; i < embeddingsNode.size(); i++) {
                    embedding[i] = embeddingsNode.get(i).asDouble();
                }
                results.add(embedding);
            } catch (EmbeddingException e) {
                throw e;
            } catch (Exception e) {
                throw new EmbeddingException("VertexAI embedding failed", e);
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
        return "vertexai";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
