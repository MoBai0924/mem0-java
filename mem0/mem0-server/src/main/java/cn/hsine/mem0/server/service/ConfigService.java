package cn.hsine.mem0.server.service;

import cn.hsine.mem0.core.embedding.EmbeddingConfig;
import cn.hsine.mem0.core.embedding.EmbeddingProviderFactory;
import cn.hsine.mem0.core.llm.LLMProviderConfig;
import cn.hsine.mem0.core.llm.LLMProviderFactory;
import cn.hsine.mem0.core.vectorstore.VectorStoreConfig;
import cn.hsine.mem0.core.vectorstore.VectorStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for configuration management.
 *
 * @author MoBai

 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final VectorStoreConfig vectorStoreConfig;
    private final EmbeddingConfig embeddingConfig;
    private final LLMProviderConfig llmProviderConfig;

    public ConfigService(VectorStoreConfig vectorStoreConfig,
                         EmbeddingConfig embeddingConfig,
                         LLMProviderConfig llmProviderConfig) {
        this.vectorStoreConfig = vectorStoreConfig;
        this.embeddingConfig = embeddingConfig;
        this.llmProviderConfig = llmProviderConfig;
    }

    /**
     * Gets the current configuration.
     *
     * @return the current configuration
     */
    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> config = new HashMap<>();

        Map<String, Object> vectorStore = new HashMap<>();
        vectorStore.put("provider", vectorStoreConfig.getProvider());
        vectorStore.put("config", vectorStoreConfig.getConfig());
        config.put("vectorStore", vectorStore);

        Map<String, Object> embedding = new HashMap<>();
        embedding.put("provider", embeddingConfig.getProvider());
        embedding.put("config", embeddingConfig.getConfig());
        config.put("embedding", embedding);

        Map<String, Object> llm = new HashMap<>();
        llm.put("provider", llmProviderConfig.getProvider());
        llm.put("config", llmProviderConfig.getConfig());
        config.put("llm", llm);

        return config;
    }

    /**
     * Updates the configuration.
     *
     * @param updates the configuration updates
     * @return the updated configuration
     */
    public Map<String, Object> updateConfig(Map<String, Object> updates) {
        log.info("Updating configuration");

        if (updates.containsKey("vectorStore")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vsConfig = (Map<String, Object>) updates.get("vectorStore");
            if (vsConfig.containsKey("provider")) {
                vectorStoreConfig.setProvider((String) vsConfig.get("provider"));
            }
            if (vsConfig.containsKey("config")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) vsConfig.get("config");
                vectorStoreConfig.setConfig(config);
            }
        }

        if (updates.containsKey("embedding")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> embConfig = (Map<String, Object>) updates.get("embedding");
            if (embConfig.containsKey("provider")) {
                embeddingConfig.setProvider((String) embConfig.get("provider"));
            }
            if (embConfig.containsKey("config")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) embConfig.get("config");
                embeddingConfig.setConfig(config);
            }
        }

        if (updates.containsKey("llm")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> llmConfig = (Map<String, Object>) updates.get("llm");
            if (llmConfig.containsKey("provider")) {
                llmProviderConfig.setProvider((String) llmConfig.get("provider"));
            }
            if (llmConfig.containsKey("config")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) llmConfig.get("config");
                llmProviderConfig.setConfig(config);
            }
        }

        return getCurrentConfig();
    }

    /**
     * Lists available providers.
     *
     * @return the available providers
     */
    public Map<String, Object> getAvailableProviders() {
        Map<String, Object> providers = new HashMap<>();
        providers.put("vectorStore", VectorStoreFactory.getSupportedProviders());
        providers.put("embedding", EmbeddingProviderFactory.getSupportedProviders());
        providers.put("llm", LLMProviderFactory.getSupportedProviders());
        return providers;
    }
}
