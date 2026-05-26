package cn.hsine.mem0.core.reranker.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.reranker.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Cohere reranker implementation.
 * Ported from Python mem0/reranker/cohere_reranker.py.
 *
 * @author MoBai

 */
public class CohereReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(CohereReranker.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public CohereReranker(String apiKey) {
        this(apiKey, "rerank-english-v3.0");
    }

    public CohereReranker(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "rerank-english-v3.0";
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK) {
        log.debug("Cohere reranking {} documents", documents.size());

        try {
            List<String> docTexts = documents.stream()
                .map(this::extractText)
                .toList();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("query", query);
            body.put("documents", docTexts);
            body.put("top_n", topK);

            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cohere.ai/v1/rerank"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Cohere rerank error: {}", response.statusCode());
                return documents;
            }

            return parseResults(response.body(), documents);

        } catch (Exception e) {
            log.error("Cohere rerank failed: {}", e.getMessage());
            return documents;
        }
    }

    @Override
    public String getName() {
        return "cohere";
    }

    private List<Map<String, Object>> parseResults(String body, List<Map<String, Object>> originalDocs) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.get("results");
        List<Map<String, Object>> reranked = new ArrayList<>();

        if (results != null && results.isArray()) {
            for (JsonNode r : results) {
                int index = r.get("index").asInt();
                double score = r.get("relevance_score").asDouble();
                Map<String, Object> doc = new LinkedHashMap<>(originalDocs.get(index));
                doc.put("rerank_score", score);
                reranked.add(doc);
            }
        }
        return reranked;
    }

    private String extractText(Map<String, Object> doc) {
        Object text = doc.get("memory");
        if (text == null) text = doc.get("text");
        if (text == null) text = doc.get("content");
        return text != null ? text.toString() : "";
    }
}
