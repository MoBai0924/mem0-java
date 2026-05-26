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
 * Milvus vector store implementation using REST API.
 *
 * @author MoBai

 */
public class MilvusVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String collectionName;

    public MilvusVectorStore(String baseUrl, String collectionName) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:19530";
        this.collectionName = collectionName != null ? collectionName : "mem0";
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
        log.info("Initialized Milvus vector store: {}/{}", this.baseUrl, this.collectionName);
    }

    @Override
    public void createCollection(String name, int vectorSize, DistanceMetric metric) { /* Milvus collections created via SDK */ }

    @Override
    public boolean collectionExists(String name) {
        return true;
    }

    @Override
    public void deleteCollection(String name) { /* Milvus collections dropped via SDK */ }

    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {
        try {
            StringBuilder body = new StringBuilder("{\"collectionName\":\"").append(collectionName).append("\",\"data\":[");
            for (int i = 0; i < vectors.size(); i++) {
                if (i > 0) {
                    body.append(",");
                }
                body.append("{\"id\":\"").append(ids.get(i)).append("\",");
                body.append("\"vector\":").append(vectorToJson(vectors.get(i))).append(",");
                body.append("\"payload\":").append(mapToJson(payloads.get(i))).append("}");
            }
            body.append("]}");
            post("/v2/vectordb/entities/insert", body.toString());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to insert into Milvus", e);
        }
    }

    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        try {
            StringBuilder body = new StringBuilder("{\"collectionName\":\"").append(collectionName).append("\",");
            body.append("\"data\":[").append(vectorToJson(queryVector)).append("],");
            body.append("\"limit\":").append(topK).append(",\"outputFields\":[\"payload\"]}");
            String resp = post("/v2/vectordb/entities/search", body.toString());
            JsonNode root = objectMapper.readTree(resp);
            List<SearchResult> results = new ArrayList<>();
            JsonNode data = root.at("/data");
            if (data.isArray()) {
                for (JsonNode d : data) {
                    JsonNode jsonNode = d.get("payload");
                    String hash = "";
                    if (jsonNode.has("hash")) {
                        hash = String.valueOf(jsonNode.get("hash"));
                    }
                    results.add(new SearchResult(d.get("id").asText(), d.get("distance").asDouble(),hash, jsonToMap(jsonNode)));
                }
            }
            return results;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to search Milvus", e);
        }
    }

    @Override
    public Optional<VectorEntry> get(String id) {
        return Optional.empty();
    }

    @Override
    public void update(String id, Double[] vector, Map<String, Object> payload) {
        insert(List.<Double[]>of(vector), List.of(payload), List.of(id));
    }

    @Override
    public void delete(String id) {
        try {
            post("/v2/vectordb/entities/delete", "{\"collectionName\":\"" + collectionName + "\",\"id\":\"" + id + "\"}");
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete from Milvus", e);
        }
    }

    @Override
    public List<VectorEntry> list(Map<String, Object> filters, Integer limit) {
        return new ArrayList<>();
    }

    @Override
    public void reset() { /* Drop and recreate */ }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public String getName() {
        return "milvus";
    }

    private String post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new VectorStoreException("Milvus error: " + resp.statusCode());
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
