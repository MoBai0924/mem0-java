package com.mem0.core.llm.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAILLMTest {

    @Test
    @DisplayName("constructor - creates with API key and defaults")
    void constructorWithApiKey() {
        OpenAILLM llm = new OpenAILLM("sk-test123", "https://api.openai.com/v1", "gpt-4o-mini");
        assertEquals("openai", llm.getName());
    }
}

class AnthropicLLMTest {

    @Test
    @DisplayName("constructor - creates with API key and defaults")
    void constructorWithApiKey() {
        AnthropicLLM llm = new AnthropicLLM("sk-ant-test123", "https://api.anthropic.com", "claude-3-5-sonnet-20241022");
        assertEquals("anthropic", llm.getName());
    }
}

class AzureOpenAILLMTest {

    @Test
    @DisplayName("constructor - creates with required settings")
    void constructorRequired() {
        AzureOpenAILLM llm = new AzureOpenAILLM("azure-key", "https://test.openai.azure.com", "my-deployment", "2024-02-01");
        assertEquals("azure", llm.getName());
    }
}

class GeminiLLMTest {

    @Test
    @DisplayName("constructor - creates with API key and defaults")
    void constructorWithApiKey() {
        GeminiLLM llm = new GeminiLLM("AIza-test123", "https://generativelanguage.googleapis.com", "gemini-1.5-flash");
        assertEquals("gemini", llm.getName());
    }
}

class OllamaLLMTest {

    @Test
    @DisplayName("constructor - creates with defaults")
    void constructorDefaults() {
        OllamaLLM llm = new OllamaLLM("http://localhost:11434", "llama3");
        assertEquals("ollama", llm.getName());
    }
}
