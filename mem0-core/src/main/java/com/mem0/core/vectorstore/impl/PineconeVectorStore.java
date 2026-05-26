package com.mem0.core.vectorstore.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.exception.VectorStoreException;
import com.mem0.core.vectorstore.DistanceMetric;
import com.mem0.core.vectorstore.SearchResult;
import com.mem0.core.vectorstore.VectorEntry;
import com.mem0.core.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Pinecone vector store implementation using REST API.
 *
 * @author MoBai

 */
public class PineconeVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PineconeVectorStore.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String indexName;

    public PineconeVectorStore(String apiKey, String baseUrl, String indexName) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.indexName = indexName != null ? indexName : "mem0";
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
        log.info("Initialized Pinecone vector store: {}", this.indexName);
    }

    @Override
    public void createCollection(String name, int vectorSize, DistanceMetric metric) { /* Pinecone indexes created via console/API */ }

    @Override
    public boolean collectionExists(String name) {
        return true;
    }

    @Override
    public void deleteCollection(String name) { /* Pinecone indexes deleted via console */ }

    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {
        try {
            StringBuilder body = new StringBuilder("{\"vectors\":[");
            for (int i = 0; i < vectors.size(); i++) {
                if (i > 0) {
                    body.append(",");
                }
                body.append("{\"id\":\"").append(ids.get(i)).append("\",");
                body.append("\"values\":").append(vectorToJson(vectors.get(i))).append(",");
                body.append("\"metadata\":").append(mapToJson(payloads.get(i))).append("}");
            }
            body.append("]}");
            post("/vectors/upsert", body.toString());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to upsert Pinecone vectors", e);
        }
    }

    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        try {
            StringBuilder body = new StringBuilder("{\"vector\":").append(vectorToJson(queryVector)).append(",\"topK\":").append(topK);
            body.append(",\"includeMetadata\":true");
            if (filters != null && !filters.isEmpty()) {
                body.append(",\"filter\":").append(mapToJson(filters));
            }
            body.append("}");
            String resp = post("/query", body.toString());
            JsonNode root = objectMapper.readTree(resp);
            List<SearchResult> results = new ArrayList<>();
            JsonNode matches = root.get("matches");
            if (matches != null && matches.isArray()) {
                for (JsonNode m : matches) {
                    JsonNode jsonNode = m.get("metadata");
                    String hash = "";
                    if (jsonNode.has("hash")) {
                        hash = String.valueOf(jsonNode.get("hash"));
                    }
                    results.add(new SearchResult(m.get("id").asText(), m.get("score").asDouble(), hash, jsonToMap(m.get("metadata"))));
                }
            }
            return results;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to query Pinecone", e);
        }
    }

    @Override
    public Optional<VectorEntry> get(String id) {
        try {
            String resp = post("/vectors/fetch", "{\"ids\":[\"" + id + "\"]}");
            JsonNode root = objectMapper.readTree(resp);
            JsonNode vectors = root.at("/vectors/" + id);
            if (vectors.isMissingNode() || vectors.isNull()) {
                return Optional.empty();
            }
            return Optional.of(new VectorEntry(id, jsonToVector(vectors.get("values")), jsonToMap(vectors.get("metadata"))));
        } catch (Exception e) {
            throw new VectorStoreException("Failed to fetch Pinecone vector", e);
        }
    }

    @Override
    public void update(String id, Double[] vector, Map<String, Object> payload) {
        insert(List.<Double[]>of(vector), List.of(payload), List.of(id));
    }

    @Override
    public void delete(String id) {
        try {
            post("/vectors/delete", "{\"ids\":[\"" + id + "\"]}");
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete Pinecone vector", e);
        }
    }

    @Override
    public List<VectorEntry> list(Map<String, Object> filters, Integer limit) {
        return new ArrayList<>();
    }

    @Override
    public void reset() {
        try {
            post("/vectors/delete", "{\"deleteAll\":true}");
        } catch (Exception ignored) {
        }
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public String getName() {
        return "pinecone";
    }

    private String post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header("Content-Type", "application/json").header("Api-Key", apiKey).POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new VectorStoreException("Pinecone error: " + resp.statusCode());
        }
        return resp.body();
    }

    private String vectorToJson(Double[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }

    private Double[] jsonToVector(JsonNode n) {
        if (n == null || !n.isArray()) {
            return new Double[0];
        }
        Double[] v = new Double[n.size()];
        for (int i = 0; i < n.size(); i++) {
            v[i] = n.get(i).asDouble();
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(JsonNode n) {
        if (n == null || !n.isObject()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.treeToValue(n, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String mapToJson(Map<String, Object> m) {
        if (m == null || m.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }
}
