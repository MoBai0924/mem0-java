package cn.hsine.mem0.core.embedding.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.embedding.EmbeddingProvider;
import cn.hsine.mem0.core.exception.EmbeddingException;
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
 * HuggingFace embedding provider implementation using Inference API.
 *
 * @author MoBai

 */
public class HuggingFaceEmbedding implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceEmbedding.class);

    private static final String DEFAULT_BASE_URL = "https://api-inference.huggingface.co/pipeline/feature-extraction";
    private static final String DEFAULT_MODEL = "BAAI/bge-small-en-v1.5";
    private static final int DEFAULT_DIMENSION = 384;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;

    public HuggingFaceEmbedding(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_DIMENSION);
    }

    public HuggingFaceEmbedding(String apiKey, String baseUrl, String model, int dimension) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();

        log.info("Initialized HuggingFace embedding provider with model: {}", this.model);
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
        log.debug("Embedding {} texts via HuggingFace", texts.size());

        try {
            StringBuilder sb = new StringBuilder("{\"inputs\":[");
            for (int i = 0; i < texts.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJson(texts.get(i))).append("\"");
            }
            sb.append("]}");

            String url = baseUrl + "/" + model;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new EmbeddingException("HuggingFace API error: " + response.statusCode());
            }

            return parseEmbeddings(response.body());

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate embeddings from HuggingFace", e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getName() {
        return "huggingface";
    }

    private List<Double[]> parseEmbeddings(String responseBody) throws Exception {
        List<Double[]> embeddings = new ArrayList<>();
        JsonNode root = objectMapper.readTree(responseBody);

        if (root.isArray()) {
            for (JsonNode item : root) {
                if (item.isArray()) {
                    // Nested array: [[0.1, 0.2, ...]]
                    Double[] embedding = new Double[item.size()];
                    for (int i = 0; i < item.size(); i++) {
                        embedding[i] = item.get(i).asDouble();
                    }
                    embeddings.add(embedding);
                }
            }
        }

        if (embeddings.isEmpty()) {
            throw new EmbeddingException("Invalid HuggingFace response format");
        }
        return embeddings;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
