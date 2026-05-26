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
 * Together AI embedding provider. Uses OpenAI-compatible API format.
 */
public class TogetherEmbedding implements EmbeddingProvider {
    private static final Logger log = LoggerFactory.getLogger(TogetherEmbedding.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;

    public TogetherEmbedding(String apiKey, String baseUrl, String model, int dimension) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.together.xyz/v1";
        this.model = model != null ? model : "togethercomputer/m2-bert-80M-8k-retrieval";
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
        try {
            StringBuilder sb = new StringBuilder("{\"model\":\"").append(model).append("\",\"input\":[");
            for (int i = 0; i < texts.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJson(texts.get(i))).append("\"");
            }
            sb.append("]}");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                    .timeout(Duration.ofSeconds(60)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new EmbeddingException("Together API error: " + response.statusCode());
            }
            return parseEmbeddings(response.body());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Together embedding failed", e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getName() {
        return "together";
    }

    private List<Double[]> parseEmbeddings(String body) throws Exception {
        List<Double[]> embeddings = new ArrayList<>();
        JsonNode data = objectMapper.readTree(body).get("data");
        if (data == null || !data.isArray()) {
            throw new EmbeddingException("Invalid response format");
        }
        for (JsonNode item : data) {
            JsonNode arr = item.get("embedding");
            Double[] embedding = new Double[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                embedding[i] = arr.get(i).asDouble();
            }
            embeddings.add(embedding);
        }
        return embeddings;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
