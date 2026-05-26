package cn.hsine.mem0.core.vectorstore.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.exception.VectorStoreException;
import cn.hsine.mem0.core.vectorstore.DistanceMetric;
import cn.hsine.mem0.core.vectorstore.SearchResult;
import cn.hsine.mem0.core.vectorstore.VectorEntry;
import cn.hsine.mem0.core.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * ChromaDB vector store implementation using REST API.
 *
 * @author MoBai

 */
public class ChromaVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(ChromaVectorStore.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String collectionName;

    public ChromaVectorStore(String baseUrl, String collectionName) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:8000";
        this.collectionName = collectionName != null ? collectionName : "mem0";
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
        log.info("Initialized ChromaDB vector store: {}/{}", this.baseUrl, this.collectionName);
    }

    @Override
    public void createCollection(String name, int vectorSize, DistanceMetric metric) {
        try {
            String body = String.format("{\"name\":\"%s\",\"get_or_create\":true}", collectionName);
            post("/api/v1/collections", body);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to create ChromaDB collection", e);
        }
    }

    @Override
    public boolean collectionExists(String name) {
        try {
            httpGet("/api/v1/collections/" + collectionName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteCollection(String name) {
        try {
            httpDelete("/api/v1/collections/" + collectionName);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete ChromaDB collection", e);
        }
    }

    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {
        try {
            StringBuilder body = new StringBuilder("{\"ids\":[");
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) {
                    body.append(",");
                }
                body.append("\"").append(ids.get(i)).append("\"");
            }
            body.append("],\"embeddings\":[");
            for (int i = 0; i < vectors.size(); i++) {
                if (i > 0) {
                    body.append(",");
                }
                body.append(vectorToJson(vectors.get(i)));
            }
            body.append("],\"metadatas\":[");
            for (int i = 0; i < payloads.size(); i++) {
                if (i > 0) {
                    body.append(",");
                }
                body.append(mapToJson(payloads.get(i)));
            }
            body.append("]}");
            post("/api/v1/collections/" + collectionName + "/add", body.toString());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to insert into ChromaDB", e);
        }
    }

    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        try {
            StringBuilder body = new StringBuilder("{\"query_embeddings\":[").append(vectorToJson(queryVector)).append("],\"n_results\":").append(topK);
            if (filters != null && !filters.isEmpty()) {
                body.append(",\"where\":").append(mapToJson(filters));
            }
            body.append("}");
            String resp = post("/api/v1/collections/" + collectionName + "/query", body.toString());
            return parseSearchResults(resp);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to search ChromaDB", e);
        }
    }

    @Override
    public Optional<VectorEntry> get(String id) {
        try {
            String body = "{\"ids\":[\"" + id + "\"],\"include\":[\"embeddings\",\"metadatas\"]}";
            String resp = post("/api/v1/collections/" + collectionName + "/get", body);
            JsonNode root = objectMapper.readTree(resp);
            JsonNode idsNode = root.get("ids");
            if (idsNode != null && idsNode.isArray() && !idsNode.isEmpty()) {
                JsonNode embeddings = root.get("embeddings");
                JsonNode metadatas = root.get("metadatas");
                Double[] vector = embeddings != null ? jsonToVector(embeddings.get(0)) : new Double[0];
                Map<String, Object> payload = metadatas != null ? jsonToMap(metadatas.get(0)) : new HashMap<>();
                return Optional.of(new VectorEntry(id, vector, payload));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new VectorStoreException("Failed to get from ChromaDB", e);
        }
    }

    @Override
    public void update(String id, Double[] vector, Map<String, Object> payload) {
        try {
            String body = String.format("{\"ids\":[\"%s\"],\"embeddings\":[%s],\"metadatas\":[%s]}",
                    id, vectorToJson(vector), mapToJson(payload));
            post("/api/v1/collections/" + collectionName + "/update", body);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to update ChromaDB", e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            post("/api/v1/collections/" + collectionName + "/delete", "{\"ids\":[\"" + id + "\"]}");
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete from ChromaDB", e);
        }
    }

    @Override
    public List<VectorEntry> list(Map<String, Object> filters, Integer limit) {
        try {
            String body = "{\"limit\":" + limit + ",\"include\":[\"embeddings\",\"metadatas\"]}";
            String resp = post("/api/v1/collections/" + collectionName + "/get", body);
            JsonNode root = objectMapper.readTree(resp);
            List<VectorEntry> entries = new ArrayList<>();
            JsonNode ids = root.get("ids");
            JsonNode embeddings = root.get("embeddings");
            JsonNode metadatas = root.get("metadatas");
            if (ids != null && ids.isArray()) {
                for (int i = 0; i < ids.size(); i++) {
                    entries.add(new VectorEntry(ids.get(i).asText(),
                            embeddings != null ? jsonToVector(embeddings.get(i)) : new Double[0],
                            metadatas != null ? jsonToMap(metadatas.get(i)) : new HashMap<>()));
                }
            }
            return entries;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to list ChromaDB", e);
        }
    }

    @Override
    public void reset() {
        deleteCollection(collectionName);
    }

    @Override
    public long count() {
        try {
            String resp = httpGet("/api/v1/collections/" + collectionName);
            JsonNode root = objectMapper.readTree(resp);
            return root.path("count").asLong(0);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String getName() {
        return "chroma";
    }

    // HTTP helpers
    private String httpGet(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header("Content-Type", "application/json").GET().timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new VectorStoreException("ChromaDB error: " + resp.statusCode());
        }
        return resp.body();
    }

    private String post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            throw new VectorStoreException("ChromaDB error: " + resp.statusCode());
        }
        return resp.body();
    }

    private void httpDelete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header("Content-Type", "application/json").DELETE().timeout(Duration.ofSeconds(30)).build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private List<SearchResult> parseSearchResults(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        List<SearchResult> results = new ArrayList<>();
        JsonNode idsArr = root.at("/ids/0");
        JsonNode distances = root.at("/distances/0");
        JsonNode metadatas = root.at("/metadatas/0");
        if (idsArr != null && idsArr.isArray()) {
            for (int i = 0; i < idsArr.size(); i++) {
                double score = distances != null ? 1.0 - distances.get(i).asDouble() : 0.0;
                Map<String, Object> payload = metadatas != null ? jsonToMap(metadatas.get(i)) : new HashMap<>();
                String hash = "";
                if (payload.containsKey("hash")) {
                    hash = String.valueOf(payload.get("hash"));
                }
                results.add(new SearchResult(idsArr.get(i).asText(), score, hash, payload));
            }
        }
        return results;
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
