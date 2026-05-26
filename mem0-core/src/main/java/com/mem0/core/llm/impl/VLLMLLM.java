package com.mem0.core.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.dto.Message;
import com.mem0.core.exception.LLMException;
import com.mem0.core.llm.LLMConfig;
import com.mem0.core.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * vLLM LLM provider implementation.
 * Uses OpenAI-compatible API format with a local vLLM server.
 */
public class VLLMLLM implements LLMProvider {
    private static final Logger log = LoggerFactory.getLogger(VLLMLLM.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;

    public VLLMLLM(String baseUrl, String model) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:8000/v1";
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        try {
            String body = buildRequestBody(messages, config);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(300));
            if (model != null) reqBuilder.header("Authorization", "Bearer dummy");
            HttpRequest request = reqBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new LLMException("vLLM API error: " + response.statusCode());
            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/choices/0/message/content").asText();
        } catch (LLMException e) { throw e; }
        catch (Exception e) { throw new LLMException("vLLM LLM failed", e); }
    }

    @Override public String getName() { return "vllm"; }

    private String buildRequestBody(List<Message> messages, LLMConfig config) throws Exception {
        var msgList = new ArrayList<>();
        for (Message m : messages) msgList.add(java.util.Map.of("role", m.role(), "content", m.content()));
        var body = new LinkedHashMap<String, Object>();
        if (model != null) body.put("model", model);
        body.put("messages", msgList);
        body.put("temperature", config.temperature());
        body.put("max_tokens", config.maxTokens());
        return objectMapper.writeValueAsString(body);
    }
}
