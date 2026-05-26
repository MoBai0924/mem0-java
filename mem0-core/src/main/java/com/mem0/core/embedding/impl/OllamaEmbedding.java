package com.mem0.core.embedding.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.embedding.EmbeddingProvider;
import com.mem0.core.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Ollama embedding provider implementation for local embedding models.
 *
 * @author MoBai

 */
public class OllamaEmbedding implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbedding.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "nomic-embed-text";
    private static final int DEFAULT_DIMENSION = 768;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    private final int dimension;

    public OllamaEmbedding() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_DIMENSION);
    }

    public OllamaEmbedding(String baseUrl, String model, int dimension) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();

        log.info("Initialized Ollama embedding provider with model: {}", this.model);
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
        log.debug("Embedding {} texts via Ollama", texts.size());

        List<Double[]> results = new ArrayList<>();
        for (String text : texts) {
            try {
                //每个embd模型的字段是有差别的，有input,有的是promot
                String requestBody = String.format("{\"model\":\"%s\",\"input\":\"%s\"}",
                    escapeJson(model), escapeJson(text));

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/embed"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new EmbeddingException("Ollama API error: " + response.statusCode());
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode embeddingArray = root.get("embeddings");
                if (embeddingArray == null || !embeddingArray.isArray()) {
                    throw new EmbeddingException("Invalid Ollama response: missing 'embedding' field");
                }

                // 先拿到外层数组里的第0个元素（真正的向量数组）
                JsonNode vectorNode = embeddingArray.get(0);

                Double[] embedding = new Double[vectorNode.size()];
                for (int i = 0; i < vectorNode.size(); i++) {
                    embedding[i] = vectorNode.get(i).doubleValue();
                }
                results.add(embedding);

            } catch (EmbeddingException e) {
                throw e;
            } catch (Exception e) {
                throw new EmbeddingException("Failed to generate embedding from Ollama", e);
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
        return "ollama";
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
