package com.mem0.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.exception.AuthenticationException;
import com.mem0.core.exception.NetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * REST client SDK for the mem0 platform API.
 * Ported from Python mem0/client/main.py MemoryClient.
 *
 * @author MoBai

 */
public class MemoryClient {

    private static final Logger log = LoggerFactory.getLogger(MemoryClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String host;
    private final String userId;

    public MemoryClient(String apiKey) {
        this(apiKey, "https://api.mem0.ai");
    }

    public MemoryClient(String apiKey, String host) {
        this.apiKey = apiKey;
        this.host = host != null ? host : "https://api.mem0.ai";
        this.userId = md5Hex(apiKey);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
        validateApiKey();
    }

    // ── Core Memory Operations ────────────────────────────────────────────

    /**
     * Add memories from messages.
     */
    public List<Map<String, Object>> add(List<Map<String, String>> messages, Map<String, Object> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", messages);
        if (options != null) body.putAll(options);
        return post("/v3/memories/add/", body);
    }

    /**
     * Get a specific memory.
     */
    public Map<String, Object> get(String memoryId) {
        return httpGet("/v1/memories/" + memoryId + "/");
    }

    /**
     * Get all memories with optional filters.
     */
    public List<Map<String, Object>> getAll(Map<String, Object> options) {
        return post("/v3/memories/", options != null ? options : Map.of());
    }

    /**
     * Search memories.
     */
    public List<Map<String, Object>> search(String query, Map<String, Object> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        if (options != null) body.putAll(options);
        return post("/v3/memories/search/", body);
    }

    /**
     * Update a memory.
     */
    public Map<String, Object> update(String memoryId, Map<String, Object> options) {
        return put("/v1/memories/" + memoryId + "/", options);
    }

    /**
     * Delete a memory.
     */
    public void delete(String memoryId) {
        httpDelete("/v1/memories/" + memoryId + "/");
    }

    /**
     * Delete all memories with optional filters.
     */
    public void deleteAll(Map<String, Object> options) {
        httpDelete("/v1/memories/");
    }

    /**
     * Get memory history.
     */
    public List<Map<String, Object>> history(String memoryId) {
        return getList("/v1/memories/" + memoryId + "/history/");
    }

    // ── Entity Operations ─────────────────────────────────────────────────

    public List<Map<String, Object>> users() {
        return getList("/v1/entities/");
    }

    public void deleteUsers(String type, String name) {
        httpDelete("/v2/entities/" + type + "/" + name + "/");
    }

    // ── Feedback ──────────────────────────────────────────────────────────

    public void feedback(String memoryId, String feedback, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("memory_id", memoryId);
        body.put("feedback", feedback);
        body.put("feedback_reason", reason);
        post("/v1/feedback/", body);
    }

    // ── Batch Operations ──────────────────────────────────────────────────

    public void batchUpdate(List<Map<String, Object>> memories) {
        put("/v1/batch/", Map.of("memories", memories));
    }

    public void batchDelete(List<String> memoryIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("memory_ids", memoryIds);
        post("/v1/batch/", body);
    }

    // ── HTTP Helpers ──────────────────────────────────────────────────────

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(host + path))
            .header("Content-Type", "application/json")
            .header("Authorization", "Token " + apiKey)
            .header("Mem0-User-ID", userId);
    }

    private List<Map<String, Object>> post(String path, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = requestBuilder(path)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(60))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponse(response);
            return parseList(response.body());
        } catch (Exception e) {
            throw new NetworkException("mem0 API request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> httpGet(String path) {
        try {
            HttpRequest request = requestBuilder(path)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponse(response);
            return parseMap(response.body());
        } catch (Exception e) {
            throw new NetworkException("mem0 API request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> put(String path, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = requestBuilder(path)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponse(response);
            return parseMap(response.body());
        } catch (Exception e) {
            throw new NetworkException("mem0 API request failed: " + e.getMessage(), e);
        }
    }

    private void httpDelete(String path) {
        try {
            HttpRequest request = requestBuilder(path)
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponse(response);
        } catch (Exception e) {
            throw new NetworkException("mem0 API request failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> getList(String path) {
        try {
            HttpRequest request = requestBuilder(path)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponse(response);
            return parseList(response.body());
        } catch (Exception e) {
            throw new NetworkException("mem0 API request failed: " + e.getMessage(), e);
        }
    }

    private void checkResponse(HttpResponse<String> response) {
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new AuthenticationException("Invalid API key or unauthorized");
        }
        if (response.statusCode() >= 400) {
            throw new NetworkException("API error " + response.statusCode() + ": " + response.body());
        }
    }

    private void validateApiKey() {
        try {
            HttpRequest request = requestBuilder("/v1/ping/")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new AuthenticationException("Invalid mem0 API key");
            }
        } catch (AuthenticationException e) { throw e; }
        catch (Exception e) { log.warn("Could not validate API key: {}", e.getMessage()); }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        if (node.isArray()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : node) result.add(objectMapper.treeToValue(item, Map.class));
            return result;
        }
        if (node.isObject() && node.has("results")) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode item : node.get("results")) result.add(objectMapper.treeToValue(item, Map.class));
            return result;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String body) throws Exception {
        return objectMapper.readValue(body, Map.class);
    }

    private static String md5Hex(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return input.hashCode() + ""; }
    }
}
