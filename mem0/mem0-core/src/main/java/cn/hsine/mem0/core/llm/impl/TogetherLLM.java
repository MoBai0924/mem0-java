package cn.hsine.mem0.core.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.dto.Message;
import cn.hsine.mem0.core.exception.LLMException;
import cn.hsine.mem0.core.llm.LLMConfig;
import cn.hsine.mem0.core.llm.LLMProvider;
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
 * Together AI LLM provider implementation.
 * Uses OpenAI-compatible API format.
 */
public class TogetherLLM implements LLMProvider {
    private static final Logger log = LoggerFactory.getLogger(TogetherLLM.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public TogetherLLM(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.together.xyz/v1";
        this.model = model != null ? model : "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo";
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        try {
            String body = buildRequestBody(messages, config);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(300))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new LLMException("Together API error: " + response.statusCode());
            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/choices/0/message/content").asText();
        } catch (LLMException e) { throw e; }
        catch (Exception e) { throw new LLMException("Together LLM failed", e); }
    }

    @Override public String getName() { return "together"; }

    private String buildRequestBody(List<Message> messages, LLMConfig config) throws Exception {
        var msgList = new ArrayList<>();
        for (Message m : messages) msgList.add(java.util.Map.of("role", m.role(), "content", m.content()));
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("messages", msgList);
        body.put("temperature", config.temperature());
        body.put("max_tokens", config.maxTokens());
        return objectMapper.writeValueAsString(body);
    }
}
