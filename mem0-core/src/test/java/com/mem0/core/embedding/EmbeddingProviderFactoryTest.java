package com.mem0.core.embedding;

import com.mem0.core.embedding.impl.*;
import com.mem0.core.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingProviderFactoryTest {

    private EmbeddingProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EmbeddingProviderFactory();
    }

    @Test
    @DisplayName("getSupportedProviders - returns all provider names")
    void getSupportedProviders() {
        String[] providers = EmbeddingProviderFactory.getSupportedProviders();
        assertEquals(7, providers.length);
        assertArrayEquals(new String[]{"openai", "ollama", "huggingface", "azure", "gemini", "together", "vertexai"}, providers);
    }

    @Test
    @DisplayName("create - creates OpenAI embedding provider")
    void createOpenAI() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("openai");
        config.setConfig(Map.of("api-key", "sk-test123"));

        EmbeddingProvider provider = factory.create(config);
        assertInstanceOf(OpenAIEmbedding.class, provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    @DisplayName("create - creates Ollama embedding provider")
    void createOllama() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("ollama");
        config.setConfig(new HashMap<>());

        EmbeddingProvider provider = factory.create(config);
        assertInstanceOf(OllamaEmbedding.class, provider);
        assertEquals("ollama", provider.getName());
    }

    @Test
    @DisplayName("create - creates HuggingFace embedding provider")
    void createHuggingFace() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("huggingface");
        config.setConfig(Map.of("api-key", "hf_test123"));

        EmbeddingProvider provider = factory.create(config);
        assertInstanceOf(HuggingFaceEmbedding.class, provider);
        assertEquals("huggingface", provider.getName());
    }

    @Test
    @DisplayName("create - creates Azure OpenAI embedding provider")
    void createAzure() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("azure");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("api-key", "azure-key");
        cfg.put("endpoint", "https://test.openai.azure.com");
        cfg.put("deployment-name", "my-deployment");
        config.setConfig(cfg);

        EmbeddingProvider provider = factory.create(config);
        assertInstanceOf(AzureOpenAIEmbedding.class, provider);
        assertEquals("azure", provider.getName());
    }

    @Test
    @DisplayName("create - creates Gemini embedding provider")
    void createGemini() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("gemini");
        config.setConfig(Map.of("api-key", "AIza-test123"));

        EmbeddingProvider provider = factory.create(config);
        assertInstanceOf(GeminiEmbedding.class, provider);
        assertEquals("gemini", provider.getName());
    }

    @Test
    @DisplayName("create - throws ConfigurationException for unknown provider")
    void createThrowsForUnknownProvider() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("unknown");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing OpenAI API key")
    void createThrowsForMissingOpenAIKey() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("openai");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing HuggingFace API key")
    void createThrowsForMissingHuggingFaceKey() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("huggingface");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Gemini API key")
    void createThrowsForMissingGeminiKey() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("gemini");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Azure endpoint")
    void createThrowsForMissingAzureEndpoint() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("azure");
        config.setConfig(Map.of("api-key", "azure-key"));

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Azure deployment name")
    void createThrowsForMissingAzureDeployment() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("azure");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("api-key", "azure-key");
        cfg.put("endpoint", "https://test.openai.azure.com");
        config.setConfig(cfg);

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - is case-insensitive for provider name")
    void createIsCaseInsensitive() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("OpenAI");
        config.setConfig(Map.of("api-key", "sk-test123"));

        EmbeddingProvider provider = factory.create(config);
        assertInstanceOf(OpenAIEmbedding.class, provider);
    }

    @Test
    @DisplayName("create - OpenAI uses default model and dimension when not specified")
    void createOpenAIDefaults() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("openai");
        config.setConfig(Map.of("api-key", "sk-test123"));

        EmbeddingProvider provider = factory.create(config);
        assertEquals(1536, provider.getDimension());
    }

    @Test
    @DisplayName("create - OpenAI uses custom model and dimension when specified")
    void createOpenAICustom() {
        EmbeddingConfig config = new EmbeddingConfig();
        config.setProvider("openai");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("api-key", "sk-test123");
        cfg.put("model", "text-embedding-3-large");
        cfg.put("dimension", 3072);
        config.setConfig(cfg);

        EmbeddingProvider provider = factory.create(config);
        assertEquals(3072, provider.getDimension());
    }
}
