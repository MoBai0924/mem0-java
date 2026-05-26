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
 * Qdrant vector store implementation using REST API.
 *
 * @author MoBai

 */
public class QdrantVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String collectionName;
    private final String apiKey;

    public QdrantVectorStore(String baseUrl, String collectionName, String apiKey) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:6333";
        this.collectionName = collectionName != null ? collectionName : "mem0";
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
        log.info("Initialized Qdrant vector store: {}/{}", this.baseUrl, this.collectionName);
    }

    @Override
    public void createCollection(String name, int vectorSize, DistanceMetric metric) {
        try {
            String distance = switch (metric) {
                case EUCLIDEAN -> "Euclid";
                case COSINE -> "Cosine";
                case INNER_PRODUCT -> "Dot";
            };
            String body = String.format("{\"vectors\":{\"size\":%d,\"distance\":\"%s\"}}", vectorSize, distance);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new VectorStoreException("Qdrant create collection error: " + resp.statusCode());
            }
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to create Qdrant collection", e);
        }
    }

    @Override
    public boolean collectionExists(String name) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName))
                    .header("Content-Type", "application/json").GET().timeout(Duration.ofSeconds(10));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to check Qdrant collection", e);
        }
    }

    @Override
    public void deleteCollection(String name) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName))
                    .header("Content-Type", "application/json").DELETE().timeout(Duration.ofSeconds(30));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete Qdrant collection", e);
        }
    }

    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {
        try {
            StringBuilder points = new StringBuilder("{\"points\":[");
            for (int i = 0; i < vectors.size(); i++) {
                if (i > 0) {
                    points.append(",");
                }
                points.append("{\"id\":\"").append(ids.get(i)).append("\",");
                points.append("\"vector\":").append(vectorToJson(vectors.get(i))).append(",");
                points.append("\"payload\":").append(mapToJson(payloads.get(i))).append("}");
            }
            points.append("]}");
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(points.toString()))
                    .timeout(Duration.ofSeconds(30));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new VectorStoreException("Qdrant insert error: " + resp.statusCode());
            }
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to insert into Qdrant", e);
        }
    }

    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        try {
            StringBuilder body = new StringBuilder("{\"vector\":").append(vectorToJson(queryVector)).append(",");
            body.append("\"limit\":").append(topK).append(",\"with_payload\":true");
            if (filters != null && !filters.isEmpty()) {
                body.append(",\"filter\":{\"must\":[");
                boolean first = true;
                for (var e : filters.entrySet()) {
                    if (!first) {
                        body.append(",");
                    }
                    body.append("{\"key\":\"").append(e.getKey()).append("\",\"match\":{\"value\":");
                    body.append("\"").append(e.getValue()).append("\"}}");
                    first = false;
                }
                body.append("]}");
            }
            body.append("}");
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(30));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new VectorStoreException("Qdrant search error: " + resp.statusCode());
            }
            return parseSearchResults(resp.body());
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to search Qdrant", e);
        }
    }

    @Override
    public Optional<VectorEntry> get(String id) {
        try {
            String body = "{\"ids\":[\"" + id + "\"],\"with_payload\":true,\"with_vectors\":true}";
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode points = root.get("result");
            if (points != null && points.isArray() && !points.isEmpty()) {
                JsonNode point = points.get(0);
                return Optional.of(new VectorEntry(id, jsonToVector(point.get("vector")), jsonToMap(point.get("payload"))));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new VectorStoreException("Failed to get from Qdrant", e);
        }
    }

    @Override
    public void update(String id, Double[] vector, Map<String, Object> payload) {
        insert(List.<Double[]>of(vector), List.of(payload), List.of(id));
    }

    @Override
    public void delete(String id) {
        try {
            String body = "{\"points\":[\"" + id + "\"]}";
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete from Qdrant", e);
        }
    }

    @Override
    public List<VectorEntry> list(Map<String, Object> filters, Integer limit) {
        try {
            String body = "{\"limit\":" + limit + ",\"with_payload\":true,\"with_vectors\":true}";
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points/scroll"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode points = root.get("result");
            List<VectorEntry> entries = new ArrayList<>();
            if (points != null && points.isArray()) {
                for (JsonNode p : points) {
                    entries.add(new VectorEntry(p.get("id").asText(), jsonToVector(p.get("vector")), jsonToMap(p.get("payload"))));
                }
            }
            return entries;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to list Qdrant points", e);
        }
    }

    @Override
    public void reset() {
        deleteCollection(collectionName);
    }

    @Override
    public long count() {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName))
                    .header("Content-Type", "application/json").GET().timeout(Duration.ofSeconds(10));
            if (apiKey != null) {
                reqBuilder.header("api-key", apiKey);
            }
            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            return root.at("/result/points_count").asLong(0);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<SearchResult> keywordSearch(String query, Integer topK, Map<String, Object> filters) {
        // Qdrant supports full-text search with payload indexes
        throw new UnsupportedOperationException("Keyword search not yet supported for Qdrant");
    }

    @Override
    public String getName() {
        return "qdrant";
    }

    // Helpers
    private List<SearchResult> parseSearchResults(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.get("result");
        List<SearchResult> list = new ArrayList<>();
        if (results != null && results.isArray()) {
            for (JsonNode r : results) {
                JsonNode jsonNode = r.get("payload");
                String hash = "";
                if (jsonNode.has("hash")) {
                    hash = String.valueOf(jsonNode.get("hash"));
                }
                list.add(new SearchResult(r.get("id").asText(), r.get("score").asDouble(), hash, jsonToMap(r.get("payload"))));
            }
        }
        return list;
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

    private Double[] jsonToVector(JsonNode node) {
        if (node == null || !node.isArray()) {
            return new Double[0];
        }
        Double[] v = new Double[node.size()];
        for (int i = 0; i < node.size(); i++) {
            v[i] = node.get(i).asDouble();
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.treeToValue(node, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
