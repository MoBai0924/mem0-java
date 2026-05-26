package com.mem0.core.llm;

import com.mem0.core.exception.ConfigurationException;
import com.mem0.core.llm.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LLMProviderFactoryTest {

    private LLMProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LLMProviderFactory();
    }

    @Test
    @DisplayName("getSupportedProviders - returns all provider names")
    void getSupportedProviders() {
        String[] providers = LLMProviderFactory.getSupportedProviders();
        assertTrue(providers.length >= 9);
        assertArrayEquals(new String[]{"openai", "anthropic", "azure", "gemini", "ollama", "deepseek", "groq", "together", "vllm"}, providers);
    }

    @Test
    @DisplayName("create - creates OpenAI LLM provider")
    void createOpenAI() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("openai");
        config.setConfig(Map.of("api-key", "sk-test123"));

        LLMProvider provider = factory.create(config);
        assertInstanceOf(OpenAILLM.class, provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    @DisplayName("create - creates Anthropic LLM provider")
    void createAnthropic() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("anthropic");
        config.setConfig(Map.of("api-key", "sk-ant-test123"));

        LLMProvider provider = factory.create(config);
        assertInstanceOf(AnthropicLLM.class, provider);
        assertEquals("anthropic", provider.getName());
    }

    @Test
    @DisplayName("create - creates Azure OpenAI LLM provider")
    void createAzure() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("azure");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("api-key", "azure-key");
        cfg.put("endpoint", "https://test.openai.azure.com");
        cfg.put("deployment-name", "my-deployment");
        config.setConfig(cfg);

        LLMProvider provider = factory.create(config);
        assertInstanceOf(AzureOpenAILLM.class, provider);
        assertEquals("azure", provider.getName());
    }

    @Test
    @DisplayName("create - creates Gemini LLM provider")
    void createGemini() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("gemini");
        config.setConfig(Map.of("api-key", "AIza-test123"));

        LLMProvider provider = factory.create(config);
        assertInstanceOf(GeminiLLM.class, provider);
        assertEquals("gemini", provider.getName());
    }

    @Test
    @DisplayName("create - creates Ollama LLM provider")
    void createOllama() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("ollama");
        config.setConfig(new HashMap<>());

        LLMProvider provider = factory.create(config);
        assertInstanceOf(OllamaLLM.class, provider);
        assertEquals("ollama", provider.getName());
    }

    @Test
    @DisplayName("create - throws ConfigurationException for unknown provider")
    void createThrowsForUnknownProvider() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("unknown");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing OpenAI API key")
    void createThrowsForMissingOpenAIKey() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("openai");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Anthropic API key")
    void createThrowsForMissingAnthropicKey() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("anthropic");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Gemini API key")
    void createThrowsForMissingGeminiKey() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("gemini");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Azure endpoint")
    void createThrowsForMissingAzureEndpoint() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("azure");
        config.setConfig(Map.of("api-key", "azure-key"));

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - is case-insensitive for provider name")
    void createIsCaseInsensitive() {
        LLMProviderConfig config = new LLMProviderConfig();
        config.setProvider("ANTHROPIC");
        config.setConfig(Map.of("api-key", "sk-ant-test123"));

        LLMProvider provider = factory.create(config);
        assertInstanceOf(AnthropicLLM.class, provider);
    }
}
