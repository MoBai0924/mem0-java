package com.mem0.core.config.llm;

/**
 * Configuration for Ollama LLM provider.
 * Ported from Python mem0/configs/llms/ollama.py.
 *
 * @author MoBai

 */
public class OllamaLlmConfig extends BaseLlmConfig {

    private String ollamaBaseUrl;

    public OllamaLlmConfig() {
        super();
        this.ollamaBaseUrl = "http://localhost:11434";
    }

    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
}
