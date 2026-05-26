package com.mem0.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified memory configuration, ported from Python mem0/configs/base.py MemoryConfig.
 *
 * @author MoBai
 */
@Data
@Component
@ConfigurationProperties(prefix = "mem0")
public class MemoryConfig {

    private VectorStoreConfig vectorStore = new VectorStoreConfig();
    private LlmConfig llm = new LlmConfig();
    private EmbedderConfig embedder = new EmbedderConfig();
    private RerankerConfig reranker;
    private String historyDbPath = System.getProperty("user.home") + "/.mem0/history.db";
    private String version = "v1.1";
    private String customInstructions;
    private Boolean mem0Telemetry = false;

    @Data
    public static class VectorStoreConfig {
        private String provider = "qdrant";
        private Map<String, Object> config = new HashMap<>();
    }

    @Data
    public static class LlmConfig {
        private String provider = "openai";
        private Map<String, Object> config = new HashMap<>();
    }

    @Data
    public static class EmbedderConfig {
        private String provider = "openai";
        private Map<String, Object> config = new HashMap<>();
    }

    @Data
    public static class RerankerConfig {
        private String provider = "cohere";
        private Map<String, Object> config = new HashMap<>();
    }
}
