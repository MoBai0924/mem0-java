package com.mem0.core.config.llm;

/**
 * Configuration for Anthropic LLM provider.
 * Ported from Python mem0/configs/llms/anthropic.py.
 *
 * @author MoBai

 */
public class AnthropicLlmConfig extends BaseLlmConfig {

    private String anthropicBaseUrl;

    public AnthropicLlmConfig() {
        super();
        this.anthropicBaseUrl = "https://api.anthropic.com/v1";
    }

    public String getAnthropicBaseUrl() { return anthropicBaseUrl; }
    public void setAnthropicBaseUrl(String anthropicBaseUrl) { this.anthropicBaseUrl = anthropicBaseUrl; }
}
