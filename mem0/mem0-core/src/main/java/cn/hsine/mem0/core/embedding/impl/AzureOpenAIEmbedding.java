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
 * Azure OpenAI embedding provider implementation.
 *
 * @author MoBai

 */
public class AzureOpenAIEmbedding implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAIEmbedding.class);

    private static final int DEFAULT_DIMENSION = 1536;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String endpoint;
    private final String deploymentName;
    private final String apiVersion;
    private final int dimension;

    public AzureOpenAIEmbedding(String apiKey, String endpoint, String deploymentName) {
        this(apiKey, endpoint, deploymentName, "2024-02-01", DEFAULT_DIMENSION);
    }

    public AzureOpenAIEmbedding(String apiKey, String endpoint, String deploymentName,
                                 String apiVersion, int dimension) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.deploymentName = deploymentName;
        this.apiVersion = apiVersion != null ? apiVersion : "2024-02-01";
        this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();

        log.info("Initialized Azure OpenAI embedding provider with deployment: {}", this.deploymentName);
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
        log.debug("Embedding {} texts via Azure OpenAI", texts.size());

        try {
            StringBuilder sb = new StringBuilder("{\"input\":[");
            for (int i = 0; i < texts.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJson(texts.get(i))).append("\"");
            }
            sb.append("]}");

            String url = String.format("%s/openai/deployments/%s/embeddings?api-version=%s",
                endpoint, deploymentName, apiVersion);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new EmbeddingException("Azure OpenAI API error: " + response.statusCode());
            }

            return parseEmbeddings(response.body());

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate embeddings from Azure OpenAI", e);
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getName() {
        return "azure";
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
