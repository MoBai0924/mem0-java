package com.mem0.core.reranker.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.reranker.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Zero Entropy reranker implementation.
 * Uses the ZeroEntropy cloud API for reranking.
 * Ported from Python mem0/reranker/zero_entropy_reranker.py.
 *
 * @author MoBai

 */
public class ZeroEntropyReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(ZeroEntropyReranker.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public ZeroEntropyReranker(String apiKey) {
        this(apiKey, "zerank-1");
    }

    public ZeroEntropyReranker(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "zerank-1";
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK) {
        log.debug("ZeroEntropy reranking {} documents", documents.size());

        if (documents.isEmpty()) {
            return documents;
        }

        try {
            List<String> docTexts = documents.stream()
                .map(this::extractText)
                .toList();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("query", query);
            body.put("documents", docTexts);

            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.zeroentropy.dev/v1/rerank"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("ZeroEntropy rerank error: {} - {}", response.statusCode(), response.body());
                return documents;
            }

            return parseResults(response.body(), documents, topK);

        } catch (Exception e) {
            log.error("ZeroEntropy rerank failed: {}", e.getMessage());
            return documents;
        }
    }

    @Override
    public String getName() {
        return "zero_entropy";
    }

    private List<Map<String, Object>> parseResults(String body, List<Map<String, Object>> originalDocs, int topK) throws Exception {
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

        reranked.sort((a, b) -> Double.compare(
            (Double) b.getOrDefault("rerank_score", 0.0),
            (Double) a.getOrDefault("rerank_score", 0.0)
        ));

        return reranked.size() <= topK ? reranked : reranked.subList(0, topK);
    }

    private String extractText(Map<String, Object> doc) {
        Object text = doc.get("memory");
        if (text == null) text = doc.get("text");
        if (text == null) text = doc.get("content");
        return text != null ? text.toString() : "";
    }
}
