package com.mem0.core.embedding.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIEmbeddingTest {

    @Test
    @DisplayName("constructor - creates with API key only")
    void constructorWithApiKey() {
        OpenAIEmbedding embedding = new OpenAIEmbedding("sk-test123");
        assertEquals("openai", embedding.getName());
        assertEquals(1536, embedding.getDimension());
    }

    @Test
    @DisplayName("constructor - creates with custom settings")
    void constructorWithCustomSettings() {
        OpenAIEmbedding embedding = new OpenAIEmbedding("sk-test", "https://custom.api.com/v1", "text-embedding-3-large", 3072);
        assertEquals("openai", embedding.getName());
        assertEquals(3072, embedding.getDimension());
    }

    @Test
    @DisplayName("constructor - uses defaults for null values")
    void constructorDefaultsForNull() {
        OpenAIEmbedding embedding = new OpenAIEmbedding("sk-test", null, null, 0);
        assertEquals(1536, embedding.getDimension());
    }
}

class OllamaEmbeddingTest {

    @Test
    @DisplayName("constructor - creates with defaults")
    void constructorDefaults() {
        OllamaEmbedding embedding = new OllamaEmbedding();
        assertEquals("ollama", embedding.getName());
        assertEquals(768, embedding.getDimension());
    }

    @Test
    @DisplayName("constructor - creates with custom settings")
    void constructorCustom() {
        OllamaEmbedding embedding = new OllamaEmbedding("http://custom:11434", "mxbai-embed-large", 1024);
        assertEquals("ollama", embedding.getName());
        assertEquals(1024, embedding.getDimension());
    }
}

class HuggingFaceEmbeddingTest {

    @Test
    @DisplayName("constructor - creates with API key only")
    void constructorWithApiKey() {
        HuggingFaceEmbedding embedding = new HuggingFaceEmbedding("hf_test123");
        assertEquals("huggingface", embedding.getName());
        assertEquals(384, embedding.getDimension());
    }

    @Test
    @DisplayName("constructor - creates with custom settings")
    void constructorCustom() {
        HuggingFaceEmbedding embedding = new HuggingFaceEmbedding("hf_test", "https://custom.api.co", "BAAI/bge-large-en-v1.5", 1024);
        assertEquals("huggingface", embedding.getName());
        assertEquals(1024, embedding.getDimension());
    }
}

class AzureOpenAIEmbeddingTest {

    @Test
    @DisplayName("constructor - creates with required settings")
    void constructorRequired() {
        AzureOpenAIEmbedding embedding = new AzureOpenAIEmbedding("azure-key", "https://test.openai.azure.com", "my-deployment");
        assertEquals("azure", embedding.getName());
        assertEquals(1536, embedding.getDimension());
    }

    @Test
    @DisplayName("constructor - creates with all settings")
    void constructorAll() {
        AzureOpenAIEmbedding embedding = new AzureOpenAIEmbedding("azure-key", "https://test.openai.azure.com", "my-deployment", "2024-06-01", 3072);
        assertEquals("azure", embedding.getName());
        assertEquals(3072, embedding.getDimension());
    }
}

class GeminiEmbeddingTest {

    @Test
    @DisplayName("constructor - creates with API key only")
    void constructorWithApiKey() {
        GeminiEmbedding embedding = new GeminiEmbedding("AIza-test123");
        assertEquals("gemini", embedding.getName());
        assertEquals(768, embedding.getDimension());
    }

    @Test
    @DisplayName("constructor - creates with custom settings")
    void constructorCustom() {
        GeminiEmbedding embedding = new GeminiEmbedding("AIza-test", "https://custom.googleapis.com", "text-embedding-004", 256);
        assertEquals("gemini", embedding.getName());
        assertEquals(256, embedding.getDimension());
    }
}
